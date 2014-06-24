module Skadistats.Spectre.Util where

import qualified Data.Text as T

import Control.Monad.Error

type MatchID = Int

instance Error T.Text where
  strMsg = T.pack

pamf :: Functor f => f a -> (a -> b) -> f b
pamf = flip fmap

fromMaybe :: a -> Maybe b -> Either a b
fromMaybe _ (Just x) = Right x
fromMaybe msg _ = Left msg

upT :: Monad m => Either a b -> ErrorT a m b
upT = ErrorT . return

mapL :: (a -> b) -> Either a c -> Either b c
mapL f (Left x) = Left (f x)
mapL f (Right x) = Right x
