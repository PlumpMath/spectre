{-# LANGUAGE OverloadedStrings #-}
module Skadistats.Spectre.AspectReader (AspectReader(..), openAspect, readMessage, allMessages) where

import Control.Monad (when)
import Control.Monad.Error
import Control.Exception (catch)
import Data.Maybe (isNothing)
import Data.Functor ((<$>))

import qualified Data.ByteString as BS
import qualified Data.ByteString.Lazy as LBS
import qualified Data.ByteString.Char8 as Char8
import qualified Data.Text as T
import qualified Data.Text.Encoding as T

import qualified Codec.Compression.GZip as GZip

import qualified Data.Binary.Get as G
import Data.Bits (testBit, clearBit, shiftL, (.|.))
import qualified Data.Sequence as S

import qualified Text.ProtocolBuffers.Basic as P
import qualified Text.ProtocolBuffers.Get as P
import qualified Text.ProtocolBuffers.WireMessage as P
import Text.ProtocolBuffers.WireMessage (messageGet, Wire)
import Text.ProtocolBuffers.Reflections (ReflectDescriptor)

import Skadistats.Spectre.Proto.Internal.StringTable (StringTable(..))

import Skadistats.Spectre.S3
import Skadistats.Spectre.Util

data AspectReader a = AspectReader
                      { aspectName    :: !T.Text
                      , stringTable   :: S.Seq T.Text
                      , matchID       :: !MatchID
                      , currentTick   :: !Int
                      , remainingData :: !LBS.ByteString
                      } deriving (Show, Eq)

toLazyBytestring :: BS.ByteString -> LBS.ByteString
toLazyBytestring x = LBS.fromChunks [x]

msgTick = 0
msgStrings = 1000

openAspect :: S3Con -> T.Text -> T.Text -> Int -> ErrorT T.Text IO (AspectReader a)
openAspect con bucket aspectName matchID = do
  let objName = aspectName `T.append` "/" `T.append` T.pack (show matchID)
  eContent <- toLazyBytestring <$> readS3Object con bucket objName
  st <- upT $ readStringTable eContent
  let content = GZip.decompress eContent
  let ar = AspectReader aspectName st matchID 0 content
  return . fst $ readReplayID ar

incrTick :: AspectReader a -> AspectReader a
incrTick ar = ar { currentTick = 1 + currentTick ar }

readMessage :: (ReflectDescriptor a, Wire a) => AspectReader a
               -> Either T.Text (Maybe (AspectReader a, a))
readMessage ar
  | not hsNxt = Right Nothing
  | otherwise = mapL T.pack eMsg
                `pamf` \msg -> Just (ar', msg)
  where ct = currentTick ar
        remData = remainingData ar
        ((hsNxt, ct'), remData', _) = G.runGetState (getHeader ct) remData 0
        (msgSize, remData'', _) = G.runGetState getVarInt remData' 0
        eMsg = fst <$> messageGet (LBS.take (fromIntegral msgSize) remData'')
        remData''' = LBS.drop (fromIntegral msgSize) remData''
        ar' = ar { currentTick = ct', remainingData = remData''' }

allMessages :: (ReflectDescriptor a, Wire a) => AspectReader a -> Either T.Text [a]
allMessages ar = case readMessage ar of -- This could be rewritten using pamf
  Left err -> Left err
  Right Nothing -> Right []
  Right (Just (ar', msg)) -> (msg:) <$> allMessages ar'

readReplayID :: AspectReader a -> (AspectReader a, Int)
readReplayID ar = (ar { remainingData = remData }, replayID)
  where (replayID, remData, _) = G.runGetState getVarInt (remainingData ar) 0

getHeader :: Int -> G.Get (Bool, Int)
getHeader ct = do
  msgType <- getVarInt
  if msgType == msgTick
    then getVarInt >>= getHeader
    else return (msgType /= msgStrings, ct)

readStringTable :: LBS.ByteString -> Either T.Text (S.Seq T.Text)
readStringTable bs = case messageGet (G.runGet getSTRaw bs) of
  Right (msg, _) -> Right . fmap (T.pack . P.uToString) . value $ msg
  Left err -> Left . T.pack $ err

getSTRaw :: G.Get LBS.ByteString
getSTRaw = do
  aspectSz <- fromIntegral <$> G.remaining
  offsetVal <- fromIntegral <$> G.lookAhead (G.skip (aspectSz - 4) >> G.getWord32be)
  -- assert offsetVal < len

  let stSz = offsetVal - 4
      start = aspectSz - offsetVal
  GZip.decompress <$> G.lookAhead (G.skip start >> G.getLazyByteString (fromIntegral stSz))

getVarInt :: G.Get Int
getVarInt = G.getWord8 >>= getVarInt'
  where getVarInt' n
          | testBit n 7 = do
            m <- G.getWord8 >>= getVarInt'
            return $ shiftL m 7 .|. clearBit (fromIntegral n) 7
          | otherwise = return $ fromIntegral n
