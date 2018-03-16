# Wowza Module installation

1. In the project root directory run ```mvn package```
2. Copy all JAR files from ```target``` directory to ```/usr/local/WowzaStreamingEngine/lib```
3. Modify ```/usr/local/WowzaStreamingEngine/conf/live/Application.xml``` to include following information:

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
                <Property>
                    <Name>uploadReferer</Name>
                    <Value>http://logic-design.jp/</Value>
                    <Type>String</Type>
                </Property>
                <Property>
                    <Name>pushHost</Name>
                    <Value>publish52.videog.jp</Value>
                    <Type>String</Type>
                </Property>
                <!--
                <Property>
                    <Name>uploadOverrideEndpoint</Name>
                    <Value>https://www.videog.jp/system/api/widget/upload_api_test.php</Value>
                    <Type>String</Type>
                </Property>
                -->
                ...
            </Properties>
            ...        
        </Application>
    </Root>
```

4. Modify ```/usr/local/WowzaStreamingEngine/conf/VHost.xml``` to include following information:

```xml
    <Root>
        <VHost>
            <HostPortList>
                <HostPort>
                    ...
                    <Port>1935</Port>
                    ...
                    <HTTPProviders>
                        ...
                        <HTTPProvider>
                            <BaseClass>com.tsuyoshihayashi.wowza.RecorderControl</BaseClass>
                            <RequestFilters>recordctrl*</RequestFilters>
                            <AuthenticationMethod>none</AuthenticationMethod>
                        </HTTPProvider>
                        <HTTPProvider>
                            <BaseClass>com.tsuyoshihayashi.wowza.FileControl</BaseClass>
                            <RequestFilters>filectrl*</RequestFilters>
                            <AuthenticationMethod>none</AuthenticationMethod>
                        </HTTPProvider>
                        <HTTPProvider>
                            <BaseClass>com.tsuyoshihayashi.wowza.CameraControl</BaseClass>
                            <RequestFilters>cameractrl*</RequestFilters>
                            <AuthenticationMethod>none</AuthenticationMethod>
                        </HTTPProvider>
                        <HTTPProvider>
                            <BaseClass>com.tsuyoshihayashi.wowza.StreamInfoControl</BaseClass>
                            <RequestFilters>stream_info*</RequestFilters>
                            <AuthenticationMethod>none</AuthenticationMethod>
                        </HTTPProvider>
                        <HTTPProvider>
                            <BaseClass>com.tsuyoshihayashi.wowza.PublishControl</BaseClass>
                            <RequestFilters>publishctl*</RequestFilters>
                            <AuthenticationMethod>none</AuthenticationMethod>                           
                        </HTTPProvider>
                        ...
                    </HTTPProviders>
                </HostPort>
            </HostPortList>
        </VHost>
    </Root>
```

5. Modify `/usr/local/WowzaStreamingEngine/conf/Server.xml` to include following information:

```xml
    <Root>
        <Server>
            ...
            <ServerListeners>
                ...
                <ServerListener>
                    <BaseClass>com.tsuyoshihayashi.wowza.ServerListener</BaseClass>
                </ServerListener>
            </ServerListeners>
            ...
            <Properties>
                ...
                <Property>
                    <Name>maxFileAge</Name>
                    <Value>3</Value>
                    <Type>Integer</Type>
                </Property>
            </Properties>
            ...
        </Server>
    </Root>
```

6. Restart Wowza

# Automatic deployment

Alternatively, steps 1–2 could be replaced with automatic build and deploy using maven profiles. One profile for deployment from a developer machine is included in the [`pom.xml`](https://github.com/TsuyoshiHayashi/wowza/blob/master/pom.xml) file. It allows for the project to be deployed to `rec.publish52.videog.jp` server. Before use, SSH private key file for this server must be present at `~/.ssh/wowza_record1.pem` path. The command to launch build & deploy process is `mvn deploy -P local`.