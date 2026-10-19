#!/bin/bash
links=(
  "https://youtu.be/a-s_8UrCw2U"
  "https://youtu.be/_P5vR9pz5Hc"
  "https://youtu.be/g1GHX-qzuTA"
  "https://youtube.com/shorts/xCywO4iYQw4"
  "https://youtu.be/tcPKQ3fuIVU"
  "https://youtu.be/kpwNjdEPz7E"
  "https://youtu.be/Jw3tIMZ2ilc"
  "https://youtu.be/GTXUWhmjCog"
  "https://youtu.be/r22LOoR9fcc"
  "https://youtu.be/BtkkXNWHrN8"
  "https://youtube.com/shorts/GFi2UhrhrHI"
  "https://youtu.be/0g40DZNLQe8"
  "https://youtube.com/shorts/Xhd-KiWkRbc"
)

echo "=========================================================="
echo "          CHECKING ALL 13 YOUTUBE LINKS IN TERMUX         "
echo "=========================================================="

idx=1
for url in "${links[@]}"; do
  echo -n "Checking [$idx/13]: $url ... "
  title=$(yt-dlp --get-title --no-warnings "$url" 2>/dev/null)
  if [ -n "$title" ]; then
    echo "OK"
    echo "   -> Title: $title"
  else
    echo "FAILED / UNAVAILABLE"
  fi
  idx=$((idx + 1))
done

echo "=========================================================="
echo "Check Completed!"
