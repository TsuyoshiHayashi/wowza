# Wowza Module installation

1. In the project root directory run ```mvn package```
2. Copy all JAR files from ```deploy``` directory to ```/usr/local/WowzaStreamingEngine/lib```
3. Modify ```/usr/local/WowzaStreamingEngine/conf/<YOUR_APP>/Application.xml``` to include following information:

```xml
    <Root>
        <Application>
            ...
            <Modules>
                ...
                <Module>
                    <Name>RecordUploader</Name>
                    <Description>Uploads recorded streams</Description>
                    <Class>com.tsuyoshihayashi.wowza.Module</Class>
                </Module>
                ...
            </Modules>
            ...
            <Properties>
                ...
                <Property>
                    <Name>apiEndpoint</Name>
                    <Value>http://www.videog.jp/system/api/ajax/w4/wowza_api_sample.php</Value>
                    <Type>String</Type>
                </Property>
                ...
            </Properties>
            ...        
        </Application>
    </Root>
```

4. Restart Wowza
