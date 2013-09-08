# zodiapp

A Clojure "entertainment" app which uses natural language processing sentiment analysis to forecast a daily outlook. The app utilizes D3 and clojurescript to display a visualization with a range of colors corresponding to whether the outlook is positive or negative. Last week of sentiment is kept and also displayed as a bar chart as you hover over a zodiac sign.

Obligatory screenshot:

![Zodiapp screen](https://raw.github.com/bass3m/zodiapp/master/doc/zodiapp.png) 

The legend at the top shows the color range going from a negative to a positive sentiment.
Each rectangle displays the zodiac sign with some additional information.
Hovering over each rectangle displays the horoscope for the day and also the score for the last week (stored using mongodb).

## Usage

[zodiapp.herokuapp.com](http://zodiapp.herokuapp.com/)

## License

Copyright © 2013 Bassem Youssef

Distributed under the Eclipse Public License, the same as Clojure.
