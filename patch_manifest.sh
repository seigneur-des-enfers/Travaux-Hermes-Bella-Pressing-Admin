sed -i 's/<meta-data/\x3c!-- \x3cmeta-data/g' app/src/main/AndroidManifest.xml
sed -i 's/android:value="YOUR_API_KEY_HERE_USE_SECRETS" \/>/android:value="YOUR_API_KEY_HERE_USE_SECRETS" \/> --\x3e/g' app/src/main/AndroidManifest.xml
