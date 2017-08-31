#!/usr/bin/python
import shutil
import glob, os, re, subprocess, smtplib
from time import strftime
from encodings import ascii
import sys
import glob, os, re, subprocess
from tv_info import *
import fnmatch
import fcntl
import select
import time
import logging
from operator import itemgetter, attrgetter

FILE_TYPES = [ "*.avi", "*.mkv", "*.mp4" ]
OUTPUT_EXTENSION = "m4v"
OUTPUT_DIRECTORY = "/Volumes/Extra/Movies/Convertidos"
HANDBRAKE_PATH = "/usr/local/bin/HandBrakeCLI"
HANDBRAKE_PRESET = "AppleTV 2"
SUBLER_PATH = "/usr/local/bin/SublerCLI"
FINAL_DIRECTORY = sys.argv[1:] if len( sys.argv ) >= 2 and sys.argv[1] else["/Users/deodoro/Torrents"]
SUBTITLE_CACHE = None
DOWNLOADS_DIRECTORY = "/Users/deodoro/Downloads"
MKVEXTRACT_PATH = "/usr/local/bin/mkvextract"
FFMPEG_PATH = "/usr/local/bin/ffmpeg"
MP4BOX_PATH = "/usr/local/bin/mp4box"
TEMP_DIR = "/Users/deodoro/.tmp/"
AC3_TEMP = os.path.join(TEMP_DIR, "audio.ac3")
AAC_TEMP = os.path.join(TEMP_DIR, "audio.aac")
VIDEO_TEMP = os.path.join(TEMP_DIR, "video.h264")
app_title = "Conversor para MP4"

# inicializacao do logger
FMT = '%(asctime)-15s %(levelname)s %(message)s'
logging.basicConfig(format=FMT, filename=os.path.join(sys.path[0], 'convert_avi.log'), level=logging.DEBUG)
logger = logging.getLogger(app_title)
# appender de console
ch = logging.StreamHandler()
ch.setLevel(logging.INFO)
ch.setFormatter(logging.Formatter(FMT))
logger.addHandler(ch)

rip_types = ["brrip","bdrip","dvdrip","r5","dvdscr","bluray","dvd.screener"]

DEBUG = False
NOTIFY = False

def run_ffmpeg(params):
    try:
        logger.debug(" ".join(params))
        p = subprocess.Popen(params, stdout= (None), stderr=subprocess.STDOUT)
        return p.wait() == 0
    except OSError:
        self.error.append("OSError: ")
    except ValueError:
        self.error.append("ValueError: Couldn't call FFMPEG with these parameters")

def run_and_await(params, silent = False):
    logger.debug(" ".join(params))
    if not DEBUG:
        with open(os.path.join(sys.path[0], "stderr.txt"), "a") as f_err:
            if silent:
                p = subprocess.Popen(params, stdout = subprocess.PIPE, stderr = f_err)
            else:
                p = subprocess.Popen(params, stdout = (None), stderr = f_err)
            ( stdout, stderr ) = p.communicate()
            return p.wait() == 0
    else:
        return True

def notify(app_title, text):    
    logger.info(text)
    if (NOTIFY and not run_and_await([ "/usr/local/bin/growlnotify", "-n", app_title,"-m", text])):
        logger.error("NOTIFICATION FAILED")
        
def process_file(filename, subtitleFilename, title, output_file):
    parts = os.path.splitext(output_file)
    temp_file = parts[0] + "_processing" + parts[1]    

    logger.info("Converting: %s" % title)
    logger.debug("Using: %s" % temp_file)
    logger.debug("To: %s" % output_file)

    if os.path.splitext(filename)[1] == ".mkv":
        # MKV, faz extracao dos streams
        logger.info("Extracting mkv")
        if (processMKV(filename, temp_file)):
            logger.info("done encoding \"%s\"" % title)
        else:
            logger.info("FAILED encoding \"%s\"" % title)
            return False
    if os.path.splitext(filename)[1] not in [".mp4", ".m4v"]:
        # Outros formatos, usa handbrake
        logger.info("Using handbrake")
        if (encodeFile(filename, temp_file)):
            logger.info("done encoding \"%s\"" % title)
        else:
            logger.info("FAILED encoding \"%s\"" % title)
            return False
    else:
        # MP4, so copia o arquivo para o alvo
        shutil.copy(filename, temp_file)
    # Movendo de temp para alvo
    logger.info("Move %s -> %s" % (temp_file, output_file))
    shutil.move(temp_file, output_file)
    # Colocando as legendas
    logger.info("Embedding subtitles")
    if (embbedSubtitle(output_file, subtitleFilename)):
        notify(app_title, "done embedding subtitle to \"%s\"" % title)
        return True
    else:
        notify(app_title, "FAILED embedding subtitle to \"%s\"" % title)
        return False

def convertible(filename, subtitleFilename):
    logger.debug("convertible? '%s'\nsubtitle '%s'" % (filename, subtitleFilename))    
    return filename and subtitleFilename and (not os.path.exists(filename)) and os.path.exists(subtitleFilename)

def embbedSubtitle(filename, subtitleFilename):    
    return run_and_await([SUBLER_PATH, '-i', subtitleFilename, '-o', filename, '-l', 'Portuguese'])
        
def encodeFile(filename, output_filename):
    return run_and_await([HANDBRAKE_PATH, '-i', filename, '-o', output_filename, '--preset=' + HANDBRAKE_PRESET, '-v0', '--no-dvdnav' ])
        
def processMKV(filename, output_filename):
    result = False
    if run_and_await([MKVEXTRACT_PATH, "tracks", filename, '1:%s' % VIDEO_TEMP, '2:%s' % AC3_TEMP ]):
        if run_ffmpeg([FFMPEG_PATH, '-i', AC3_TEMP, '-vn', '-r', '30000/1001', '-acodec', 'libfaac', '-ac', '6', '-ar', '48000', '-ab', '448k', '-y', AAC_TEMP]):
            result = run_and_await([MP4BOX_PATH, '-add', VIDEO_TEMP, '-add', AAC_TEMP, output_filename])
            os.remove(AAC_TEMP)
        os.remove(AC3_TEMP)
        os.remove(VIDEO_TEMP)
    return result
    
def addToITunes(title, filename):
    # Adicionar ao iTunes
    if not run_and_await(["osascript", "-e", itunes_script_for(filename, title)], silent = True):
        logger.error("failed adding to itunes")

def tagFile(filename):
    # Acrescentando label "green" aos itens processado
    logger.debug("tagging %s" % filename)
    if run_and_await(["osascript", "-e", label_script_for(filename)], silent = True):
        if os.path.dirname(filename) not in FINAL_DIRECTORY + [DOWNLOADS_DIRECTORY]:
            tagFile(os.path.dirname(filename))
    else:
        logger.error("could not apply label")

def traverse(baseDir, mask):
    result = []
    for root, dirs, files in os.walk(baseDir):
        for filename in [f for f in files if fnmatch.fnmatch(os.path.splitext(f)[1], mask)]:
            result.append(os.path.join(root, filename))
    return result

def findMatchingSubtitle(filePath):
    logger.debug("Looking for subtitle for %s" % filePath)
    ignoreList = ["extratorrentrg", "sample"]
    for expr in ignoreList:
        if re.search(expr, filePath, re.IGNORECASE):
            return None
    for f in subtitleFiles():
        mask = re.sub('\[', '\[', os.path.basename(os.path.splitext(f)[0]))
        if (compareRecurs(filePath, mask)):
            logger.debug("Match subtitle %s to %s" % (f, filePath))
            return f

def compareRecurs(filename, mask):
    for dir in FINAL_DIRECTORY:
        if filename == (os.path.abspath(dir) if dir.endswith("/") else dir):
            return None
    m = re.match(mask, os.path.basename(filename), re.IGNORECASE)
    return m if m else compareRecurs(os.path.dirname(filename), mask)

def pickTitle(movieFile):
    # [ UsaBit.com ] - The.Revenant.2009.BDRip.XviD-aAF/CD1/aaf-the.revenant.2009.bdrip.xvid-cd1.avi
    masks = ["^\[.+\.[a-z]{3}\s*\]\s+\-\s+((.+\.)+)[0-9]{4}.*\.(repack\.)?(%s)\." % "|".join(rip_types), 
             "^((.+\.)+)[0-9]{4}.*\.(repack\.)?(%s)\."  % "|".join(rip_types), # The.Hunter.2011.BRRip.XviD.Ac3.Feel-Free
             "^(.+)\s*[ \[(][0-9]{4}[ \])].*(repack)?\s*(%s)" % "|".join(rip_types), # Gunless [2010] DvDRiP XviD - ExtraTorrentRG
             "^(.+)[ \[(][0-9]{4}[ \])].*(repack\.)?\.[a-z]{3}", # Gunless 2010.avi
             "^(.+)\s+(%s)"  % "|".join(rip_types)] # Gunless DvDRiP XviD
    logger.debug("pickTitle(%s)" % movieFile)
    
    for mask in masks:
        m = compareRecurs(movieFile, mask)        
        logger.debug("compareRecurs(%s, %s)" % (movieFile, mask))
        if m:
            ttl = m.group(1).replace(".", " ").strip()
            part = pickPartNumber(movieFile)
            if part:
                ttl = "%s part %s" % (ttl, part)
            return ttl
    return os.path.splitext(os.path.basename(movieFile))[0]

def pickPartNumber(movieFile):
    masks = [".+/cd(\d)/.+", ".+cd(\d)\.[a-z0-9]{3}"]
    for mask in masks:
        # Caso especial de filmes de duas partes (../CD1/...)
        m = re.match(mask, movieFile, re.IGNORECASE)
        if m:
            return "%s" % m.group(1)
    return ""

def movies():
    files = []
    for item in FINAL_DIRECTORY:
        if os.path.isdir(item):
            for file_type in FILE_TYPES:
                files.extend(traverse(item, file_type))
        else:
            files.append(item)
    return files

def subtitleFiles():
    global SUBTITLE_CACHE
    if not SUBTITLE_CACHE:
        SUBTITLE_CACHE = []
        for dir in FINAL_DIRECTORY + [DOWNLOADS_DIRECTORY]:
            SUBTITLE_CACHE.extend(traverse(dir, "*.srt"))
    return SUBTITLE_CACHE

def matchFiles(allFiles):
    filesToProcess = []
    for input_file in allFiles:
        subtitle = findMatchingSubtitle(input_file)
        if subtitle:
            title = pickTitle(input_file)
            output_file = os.path.join( OUTPUT_DIRECTORY, "%s.%s" % ( os.path.basename(os.path.splitext(input_file)[0]), OUTPUT_EXTENSION ) )            
            if convertible(output_file, subtitle):
                logger.debug("Queueing - title '%s'\nfile '%s'\nsubtitle '%s'" % (title, input_file, subtitle))
                filesToProcess.append({"file" : input_file, "subtitle": subtitle, "title": title, "output": output_file})
    
    extPriority = {".mp4": "0", ".mkv": "1", ".avi": "2"}
    return sorted(filesToProcess, key=lambda x: extPriority[os.path.splitext(x["file"])[1]] + x["title"].upper())

def processFiles():
    filesToProcess = matchFiles(movies())
    
    notify(app_title, "Queued:\n%s" % "\n".join(["%s -> %s" % (i["title"], i["output"]) for i in filesToProcess]))
    for item in filesToProcess:
        if process_file(item["file"], item["subtitle"], item["title"], item["output"]):
            addToITunes(item["title"], item["output"])
            tagFile(item["file"])
            tagFile(item["subtitle"])

# processo de conversao
logger.info("Diretorio alvo: %s" % FINAL_DIRECTORY)
notify(app_title, "Inicio da conversao")
processFiles()
notify(app_title, "FIM do processo")
sys.exit(0)