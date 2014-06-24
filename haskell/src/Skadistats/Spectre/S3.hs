{-# LANGUAGE OverloadedStrings #-}
module Skadistats.Spectre.S3
       ( S3Con(..)
       , loadAwsCredentials
       , defaultS3Con
       , readS3Object
       , Aws.Credentials
       ) where

import Control.Exception (catch)
import Control.Monad.Error
import Data.Functor ((<$>))

import System.Environment (getEnv)
import System.IO (openFile, IOMode(..))
import System.IO.Strict (hGetContents)
import System.FilePath (FilePath, (</>))
import Data.IORef (newIORef)

import qualified Data.ByteString as BS
import qualified Data.ByteString.Lazy as LBS
import qualified Data.ByteString.Char8 as Char8
import qualified Data.Text as T
import qualified Data.Text.Encoding as T

import Data.Conduit (($$), unwrapResumable)
import Data.Conduit.List (consume)
import Control.Monad.Trans.Resource (runResourceT)
import Network.HTTP.Conduit (withManager, responseBody)

import qualified Data.Ini as Ini
import qualified Data.Ini.Types as Ini
import qualified Data.Ini.Reader as Ini

import qualified Aws
import qualified Aws.S3 as S3

import Skadistats.Spectre.Util

data S3Con = S3Con
             { awsCfg :: Aws.Configuration
             , s3Cfg  :: S3.S3Configuration Aws.NormalQuery
             }

awsCredentialsPath :: IO FilePath
awsCredentialsPath = getEnv "HOME" `pamf` \home ->
  home </> ".aws" </> "credentials"

credentialsProfile :: Ini.SectionName
credentialsProfile = "default"

loadAwsCredentials :: ErrorT T.Text IO Aws.Credentials
loadAwsCredentials = do
  pth <- liftIO $ awsCredentialsPath
  contents <- liftIO $ openFile pth ReadMode >>= hGetContents
  ref <- liftIO $ newIORef [] -- Required for Aws.Credentials, dunno why
  cfg <- upT . (mapL (T.pack . show))  $ Ini.parse contents
  let mID = Ini.getOption credentialsProfile "aws_access_key_id" cfg
  id <- upT $ fromMaybe "No aws_access_key_id" mID
  let mSecret = Ini.getOption credentialsProfile "aws_secret_access_key" cfg
  secret <- upT $ fromMaybe "No aws_secret_access_key" mSecret
  return (Aws.Credentials (Char8.pack id) (Char8.pack secret) ref)

defaultS3Con :: ErrorT T.Text IO S3Con
defaultS3Con =
  loadAwsCredentials >>= \cred ->
  return $ S3Con (Aws.Configuration Aws.Timestamp cred (Aws.defaultLog Aws.Info)) Aws.defServiceConfig

readS3Object :: S3Con -> S3.Bucket -> T.Text -> ErrorT T.Text IO BS.ByteString
readS3Object con bucketName objName =
  let obj :: IO BS.ByteString
      obj =
        -- Found this in an example, no idea how it works
        withManager $ \mgr -> do
          S3.GetObjectResponse { S3.gorResponse = rsp } <-
            Aws.pureAws (awsCfg con) (s3Cfg con) mgr $
              S3.getObject bucketName objName
          -- Get the response body, forget about cleanup, concat the output of a
          -- consume sink
          -- TODO: understand conduit
          unwrapResumable (responseBody rsp) >>= (\s -> fmap BS.concat $ (fst s) $$ consume)
      handler :: S3.S3Error -> IO (Either T.Text BS.ByteString)
      handler = return . Left . T.pack . show
  in ErrorT $ catch (Right <$> obj) handler
