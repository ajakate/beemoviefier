# beemoviefier

beemoviefier is a command line tool to help you create your own *"X, but every time Y happens it gets faster"* videos, originally inspired by this [gem](https://www.youtube.com/watch?v=W31e9meX9S4&t=177s).

It's been six years since this meme was popular but I think it deserves a renaissance.
For the uninitiated, it essentially starts with a movie, song, or video clip at normal speed
and slowly speeds up by multiplying factor every time a certain word is said or a certain thing happens,
often ending up unintelligible by the end.

## Dependencies

- [VLC](https://www.videolan.org/vlc/)
- [babashka](https://github.com/babashka/babashka#installation)
- [ffmpeg](https://ffmpeg.org/download.html)

## Instructions

### 1. Obtain video you want to convert
Get a local file of the video however you want, and any video format supported by ffmpeg should work fine, although so far I've only tested mp4 and webm via youtube-dl.

### 2. Open the video in VLC

Once you're watching the video, open up the bookmarks window (command + b on mac)

Every time "your word" is said, or "your special thing" happens, immediately add a new bookmark. The name of the bookmarks shouldn't matter, just the timestamps.

Once the last instance of your event happens (or periodically), go to File -> Save playlist (command + s on mac) which should make a `.m3u` file.

![Screenshot of VLC with bookmarks](docs/chimney.png)

**IMPORTANT NOTES:**
- **Make sure you save your playlist before the video finishes playing** -- while testing I noticed that if I let the video finish before hitting save it wiped all my bookmarks :(
- When you save the playlist file, make sure that your video is the only video in the playlist. beemoviefier naively grabs the first timestamps it sees via a regex match, so you could get in trouble if there is more than one video.
- You may notice a lag between when you hit *Add* and the timestamp of the bookmark. For me it seemed to be consistently about a second... beemoviefier accounts for this lag and makes it configurable.

### 3. Run the script

A simple example

`bb run original_video.webm -p vlc_playlist_file.m3u`

will run ffmpeg and create a timestamped output mp4 file in the form `out_2022_12_24_21_43_23_238439.mp4`

## API
