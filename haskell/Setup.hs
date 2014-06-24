#!/usr/bin/env runhaskell

import Control.Monad
import Control.Applicative

import Data.List
import Data.Maybe

import Data.Accessor

import Distribution.Simple
import Distribution.Simple.Setup
import Distribution.Simple.LocalBuildInfo
import Distribution.ModuleName hiding (main)
import Distribution.PackageDescription
import Distribution.Verbosity
import Distribution.Simple.Utils
import System.Process
import System.Exit
import System.FilePath
import System.Directory

protoDir :: FilePath
protoDir = "../aspectSerializer/src/main/proto"

-- accessors to make subsubrecord access a bit less mess...
accLocalPkgDescr :: Accessor LocalBuildInfo PackageDescription
accLocalPkgDescr = accessor localPkgDescr (\a r -> r {localPkgDescr = a})

-- go through Maybe hoping that it is not empty
accLibrary :: Accessor PackageDescription Library
accLibrary = accessor (fromJust . library) (\a r -> r {library = Just a})

accExposedModules :: Accessor Library [ModuleName]
accExposedModules = accessor exposedModules (\a r -> r {exposedModules = a})

replace x y = map (\c -> if c == x then y else c)

-- compile .proto file and return a list of generated modules parsed from hprotoc stdout
protoCompile :: Verbosity -> FilePath -> FilePath -> IO [String]
protoCompile v destDir src = do
  notice v $ "Compiling proto definition: " ++ src ++ " to " ++ destDir
  let args = ["-I", protoDir, "-d", destDir, src]
  notice v . filter (/= '\n') . intercalate " " $ "hprotoc": args
  (ec,out,err) <- readProcessWithExitCode "hprotoc" args ""
  when (ec /= ExitSuccess) $ die $ "hprotoc failed (" ++ show ec ++ "): " ++ err
  return $ map (replace '/' '.' . takeWhile (/= '.')) .
    catMaybes . map (stripPrefix $ destDir ++ "/") . lines $ out

-- call main configuration routine, then generate haskell sources and push their module names
-- to the list of exposed modules of the library
myConfHook :: (GenericPackageDescription, HookedBuildInfo) -> ConfigFlags -> IO LocalBuildInfo
myConfHook x cf = do
  lbi <- (confHook simpleUserHooks) x cf
  let verb = fromFlag $ configVerbosity $ cf
      destDir = buildDir lbi </> "autogen"
  protoFiles <- map (fromJust . stripPrefix (protoDir ++ "/")) . filter (".proto" `isSuffixOf`) <$> getRecursiveContents protoDir
  modList <- nub . map fromString . concat <$> mapM (protoCompile verb destDir) protoFiles
  return $ (accLocalPkgDescr ^: accLibrary ^: accExposedModules ^: (++modList)) lbi

main :: IO ()
main = let hooks = simpleUserHooks
       in defaultMainWithHooks hooks {confHook = myConfHook}

getRecursiveContents :: FilePath -> IO [FilePath]
getRecursiveContents topdir = do
  names <- getDirectoryContents topdir
  let properNames = filter (`notElem` [".", ".."]) names
  paths <- forM properNames $ \name -> do
    let path = topdir </> name
    isDirectory <- doesDirectoryExist path
    if isDirectory
      then getRecursiveContents path
      else return [path]
  return (concat paths)
