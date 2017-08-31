from xattr import xattr
from struct import unpack
import os, re

my_shows = [("30.Rock", "30 Rock"), ("Justified", "Justified"), ("Fringe", "Fringe"), \
            ("Hawaii.Five-0", "Hawaii 5-0"), ("Stargate.Universe", "Stargate Universe"), \
            ("Greys.Anatomy", "Grey's Anatomy"), ("Law.and.Order.LA", "Law & Order LA"), \
            ("Law.and.Order.SVU", "Law & Order SVU"), ("The.Event", "The Event"), \
            ("Warehouse.13", "Warehouse 13"), ("Blue.Bloods", "Blue Bloods"), ("House", "House"),
            ("The.Borgias", "The Borgias"), ("Camelot", "Camelot"), ("The.Killing", "The Killing"),
            ("Game.of.Thrones", "Game of Thrones"), ("Law.and.Order.Criminal.Intent", "Law and Order Criminal Intent"),
            ("Falling.Skies", "Falling Skies"), ("True.Blood", "True Blood"),
            ("Rome", "Rome"), ("Breaking.Bad", "Breaking Bad"), ("Bored.To.Death", "Bored to Death"),
            ("through.the.wormhole", "Through the Wormhole"), ("Ringer", "Ringer"), 
            ("Person.of.Interest", "Person of Interest"), ("Terra.Nova", "Terra Nova"),
            ("The.Walking.Dead", "The Walking Dead"), ("Pioneer.One", "Pioneer One"),
            ("Merlin", "Merlin"), ("Hell.On.Wheels", "Hell on Wheels"),
            ("Boardwalk.Empire", "Boardwalk Empire"), ("Law.and.Order.UK", "Law and Order UK"),
            ("Community", "Community"), ("Alcatraz", "Alcatraz"), ("Awake", "Awake"), ("Touch", "Touch"), ("Sinbad", "Sinbad"),
            ("Continuum", "Continuum"), ("Sons.of.Anarchy", "Sons of Anarchy"), ("Arrow", "Arrow")]
movie_script = """\
tell application "iTunes"
    set posix_path to "%s"
    set mac_path to posix_path as POSIX file
    set video to (add mac_path)
    set video kind of video to Movie
    set name of video to "%s"
end tell
""" 
tv_script = """\
tell application "iTunes"
    set posix_path to "%s"
    set mac_path to posix_path as POSIX file
    set video to ( add mac_path )
    set video kind of video to TV show  
    set show of video to "%s"
    set season number of video to %s
    set episode number of video to %s
end tell
"""
label_script = """\
tell application "Finder"
    set posix_path to "%s"
    set mac_path to posix_path as POSIX file
    set label index of %s mac_path to 6
end tell
""" 

ep_regex = r".+S(?P\d{2})E(?P\d{2}).+"

def info(f):
    d, filename = os.path.split(f)
    show = [i for i in my_shows if filename.lower().find(i[0].lower()) != -1]
    if len(show) > 0:
        m = re.match(ep_regex, filename, re.IGNORECASE)
        if m:
            ep_info = m.groupdict();
            return {'show' : show[0][1], 'season' : int(ep_info['season']), 'episode' : int(ep_info['episode'])}
    return None

def itunes_script_for(f, title):
    data = info(f)
    if data:
        return tv_script % (f, data['show'], data['season'], data['episode'])
    else:
        return movie_script % (f, title)
        
def label_script_for(f):
    return label_script % (f, os.path.isdir(f) and 'folder' or 'file')

def tag_file(f, color):
    colornames = { 0: 'none', 1: 'gray', 2 : 'green', 3 : 'purple', 4 : 'blue', 5 : 'yellow', 6 : 'red', 7 : 'orange' }

    attrs = xattr('./test.cpp')

    try:
        finder_attrs = attrs[u'com.apple.FinderInfo']
        flags = unpack(32*'B', finder_attrs)
        color = flags[9] >> 1 & 7
    except KeyError:
        color = 0

    print colornames[color]