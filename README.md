# Pasta-for-Spotify
A [material design](https://material.google.com/) Spotify client for Android.

## About

Pasta for Spotify is a material design Spotify client for android that attempts to create a better user experience using the [Spotify Web API](https://developer.spotify.com/web-api/). It was created to show an improvement in design over the official Spotify app, and to allow for older and slower devices to have quicker access to Spotify's services without losing (much) functionality. Some examples of this are as follows:
### Design:
- Touch areas are increased for small devices to be able to open things like menus and playlists more easily than the layout in the official app
- A lot of access relies on swipe navigation within the app to speed up general user experience
- The shuffle and repeat buttons have been removed from the now playing screen and moved into an "order tracks" dialog that is accessible from the menu of playlists and albums, and shows up in the settings menu. This provides greater consistency: in the official app you can order tracks by name, date, etc when viewing a list, but this becomes obsolete once shuffle is enabled. Moving all order-related options to the same place makes more sense from the perspective of both a new user and a user coming from the official app.
- The backgrounds of different elements on the screen change color according to the album arts, allowing the user to quickly identify items based on color as well as text and image, and blend the image with the rest of the app so that it doesn't seem out of place.
- Most parts of the app can be customized from the settings menu to allow the user to change their experience, including the main color scheme of the app and whether to display items as cards, tiles, or lists.
- All parts of the official app that serve as a bookmark for an item are united into one "Favorites" section of the app. This means that artists that are being followed, playlists that have been created (or followed), and albums and tracks that have been "added" show up here, making the result of the "favorite" action obvious to most users instead of having to google ["what does it mean to add a track in spotify?"](https://www.google.com/search?newwindow=1&safe=strict&q=what+does+it+mean+to+add+a+track+in+spotify%3F&oq=what+does+it+mean+to+add+a+track+in+spotify) (like I did).

### Performance:
Though I am not sure what the official Spotify client uses to switch layouts, many current users have reported a large increase in loading speed of Pasta for Spotify versus the official app. This could be because of a few things:
- The app uses Aidan Follestad's [Async](https://github.com/afollestad/async) library to load content separately from the UI thread, which allows the user to navigate the app while content is loading, for example: navigating back to the previous task while content is loading will cancel the download.
- [Butterknife](http://jakewharton.github.io/butterknife/), by Jake Wharton, is used to bind views instead of the standard view binding method. Truthfully I have no idea what this means but it saves time so just go with it. ;)
- [Glide](https://github.com/bumptech/glide) is used to load image urls provided by the spotify api. This saves a lot of loading time by asynchronously loading an image while scrolling as well as compressing it to speed up the download as much as possible.

## Features
Pasta for Spotify has almost all the features of the official app, though playlists cannot be downloaded because that could potentially create a way for users to export songs from the app, and I'm sure Spotify would not like that.
- Shows newly released albums and featured playlists
- A favorites section for playlists, albums, songs, and artists
- Search through all of Spotify's database
- View different categories of music
- Dynamic backgrounds that adapt to the album art
- A light/dark theme in the settings menu
- Options to change the global color scheme of the app
- Change the ordering of songs in playlists and albums

### Limitations
There are a few major issues with the implementation of the Spotify API, some of which are because of the API itself (meaning they cannot be fixed by me), and some are internal but require a lot of refactoring to come up with an appropriate solution. Examples:
- [The app will occasionally refuse to play a song until a restart](https://github.com/TheAndroidMaster/Pasta-for-Spotify/issues/1). The "Unknown error, please restart" toast message is specific to this issue. 
- It is not possible to download a track to play it offline. See [this issue](https://github.com/spotify/android-sdk/issues/2) for progress updates.
- The 'restart' button usually causes an authentication error resulting in the app force closing.
- The 'Sign Out' option in the settings menu will cause a crash if the official Spotify app is installed.
- There are many features that are not accessible from a free account, such as playing songs and favoriting tracks.

### Screenshots

Splash Screen | Home Screen | Now Playing
------------- | ----------- | -----------
![](http://theandroidmaster.github.io/images/screenshots/Screenshot_2016-04-17-15-33-39.png) | ![](http://theandroidmaster.github.io/images/screenshots/image4155.png) | ![](http://theandroidmaster.github.io/images/screenshots/image4646.png)

## Contributing
### Issues
Okay, there aren't really any guidelines over issue formatting provided that you don't create duplicate issues and test the app throughly before creating an issue (ex: try clearing the app data).

### Pull Requests
I usually don't have any organization over how I handle issues and what I commit at any given time. If I'm interrupted in the middle of a session, I might commit a half-finished class that causes an error before the project even compiles. To prevent good work going to waste or having to be copied and pasted a lot to prevent merge conflicts, please contact me before you start working on any changes. This way we can decide who will work on the project when, and exactly what changes they will be making.

## Links

- [Google Plus Community](https://plus.google.com/communities/101536497390778012419)
- [Website](http://theandroidmaster.github.io/apps/pasta/)

#### Contributors:
- [James Fenn](http://theandroidmaster.github.io/)
- [Jan-Lk Else](https://plus.google.com/+JanLkElse)
- [Patrick J](http://pddstudio.com/)
- [Vukašin Anđelković](https://plus.google.com/+Vuka%C5%A1inAn%C4%91elkovi%C4%87zavukodlak)
- [Kevin Aguilar](https://plus.google.com/+KevinAguilarC)

## License

```
Copyright 2016 James Fenn

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
