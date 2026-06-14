#!/data/data/com.termux/files/usr/bin/bash
DB="QDBS"             # غير الاسم هنا
OUT="database.json"

echo "{" > "$OUT"
first=1
for table in $(sqlite3 "$DB" ".tables"); do
    [ $first -eq 0 ] && echo "," >> "$OUT"
    echo "\"$table\":" >> "$OUT"
    sqlite3 -json "$DB" "SELECT * FROM $table;" >> "$OUT"
    first=0
done
echo "}" >> "$OUT"
echo "تم الحفظ في $OUT"
