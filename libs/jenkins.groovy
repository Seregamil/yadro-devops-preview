class Jenkins {
    private _pipeline
    private _login
    private _password
    private _credentials
    
    private _baseUrl
    private _jobUrl
    
    private _credentialsPool = null
    private _crumbs = null

    def Jenkins(pipeline, String buildUrl, String login, String password) {
        this._pipeline = pipeline

        this._login = login
        this._password = password

        this._jobUrl = buildUrl.substring(0, buildUrl.lastIndexOf('/job'))
        this._baseUrl = buildUrl.substring(0, buildUrl.indexOf('/job'))

        def mergedCredentials = "${ login }:${ password }"
        this._credentials = mergedCredentials.bytes.encodeBase64().toString()
    }

    def getCredentialPair() {
        return "${this._login}:${this._password}"
    }

    def getPipeline(name) {
        def maskValues = [[password: this._credentials]]
        if(this._crumbs != null)
            maskValues << [password: this._crumbs]

        this._pipeline.wrap([$class: "MaskPasswordsBuildWrapper", 
            varPasswordPairs: maskValues]) 
        {
            def headers = [
                [ name: 'Authorization', value: "Basic ${ this._credentials }" ]
            ]
            if(this._crumbs != null) 
                headers << [ name: 'Jenkins-Crumb', value: this._crumbs ]

            def response = this._pipeline.httpRequest customHeaders: headers, 
                url: "${ this._jobUrl }/job/${ name }/config.xml",
                validResponseCodes: "200:599"

            def status = response.status
            def output = getResponseMessage(status)
            if(status == 200) {               
                this._pipeline.wrap([$class: "MaskPasswordsBuildWrapper", varPasswordPairs: [[password: this._password]]]) 
                {
                    def shScript = "wget -q --auth-no-challenge --user=${ this._login } --password=${ this._password } ${ this._jobUrl }/job/${ name }/config.xml -O config.xml"
                    this._pipeline.sh(script: shScript, returnStdout: true)
                }

                output << this._pipeline.readFile (file: 'config.xml')
            }
            
            return output
        } 
    }

    def getBaseUrl() {
        return this._baseUrl
    }

    def getJobUrl() {
        return this._jobUrl
    }

    def buildWithParameters(String namespace, String jobName, parameters) {
        def args = parameters.collect { k, v -> "${ k }=${ v }" }.join('&')
        def url = "${ this._baseUrl }/job/${ namespace }/job/global/job/${ jobName }/buildWithParameters?delay=0sec&${ args }"
        def location = this.getBuildLocation(url)
        def buildUrl = this.getBuildUrl(location)
        def buildStatus = this.getBuildStatus(buildUrl)
        return [buildUrl, buildStatus]
    }

    def getConsoleOutput(String jobBuildUrl) {
        def maskValues = [[password: this._credentials]]
        if(this._crumbs != null)
            maskValues << [password: this._crumbs]

        this._pipeline.wrap([$class: "MaskPasswordsBuildWrapper", 
            varPasswordPairs: maskValues]) 
        {
            def headers = [[ name: 'Authorization', value: "Basic ${ this._credentials }" ]]
            if(this._crumbs != null) 
                headers << [ name: 'Jenkins-Crumb', value: this._crumbs ]

            def response = this._pipeline.httpRequest customHeaders: headers, 
                url: "${jobBuildUrl}/consoleText",
                validResponseCodes: "200:599"

            def status = response.status
            def content = response.content

            return content
        }
    }

    def getBuildStatus(String url) {
        def maskValues = [[password: this._credentials]]
        if(this._crumbs != null)
            maskValues << [password: this._crumbs]

        this._pipeline.wrap([$class: "MaskPasswordsBuildWrapper", 
            varPasswordPairs: maskValues]) 
        {
            def headers = [[ name: 'Authorization', value: "Basic ${ this._credentials }" ]]
            if(this._crumbs != null) 
                headers << [ name: 'Jenkins-Crumb', value: this._crumbs ]

            while(true) {
                def response = this._pipeline.httpRequest customHeaders: headers, 
                    url: "${ url }api/json",
                    validResponseCodes: "200:599"

                def status = response.status
                def json = this._pipeline.readJSON(text: response.content)

                def inProgress = json.inProgress.toBoolean()
                def building = json.building.toBoolean()
                def result = json.result

                if(building == false && inProgress == false) {
                    try {
                        if(result == null) 
                            continue
                        return result
                    } catch (Exception e) {}
                }

                this._pipeline.sleep time: 10, unit: 'SECONDS'
            }
        } 
    }

    def getBuildUrl(String location) {
        def maskValues = [[password: this._credentials]]
        if(this._crumbs != null)
            maskValues << [password: this._crumbs]

        this._pipeline.wrap([$class: "MaskPasswordsBuildWrapper", 
            varPasswordPairs: maskValues]) 
        {
            def headers = [[ name: 'Authorization', value: "Basic ${ this._credentials }" ]]
            if(this._crumbs != null) 
                headers << [ name: 'Jenkins-Crumb', value: this._crumbs ]

            def response = this._pipeline.httpRequest customHeaders: headers, 
                url: location,
                validResponseCodes: "200:599"

            def status = response.status
            def json = this._pipeline.readJSON(text: response.content)

            def url = json.executable.url
            return url
        } 
    }

    def getBuildLocation(String url) {
        def maskValues = [[password: this._credentials]]
        if(this._crumbs != null)
            maskValues << [password: this._crumbs]

        this._pipeline.wrap([$class: "MaskPasswordsBuildWrapper", 
            varPasswordPairs: maskValues]) 
        {
            def headers = [[ name: 'Authorization', value: "Basic ${ this._credentials }" ]]
            if(this._crumbs != null) 
                headers << [ name: 'Jenkins-Crumb', value: this._crumbs ]

            def response = this._pipeline.httpRequest customHeaders: headers, 
                url: url,
                httpMode: 'POST',
                validResponseCodes: "200:599"

            def status = response.status
            def location = response.headers.Location.first()
            return "${ location }api/json"
        } 
    }

    def loadCrumbs() {
        this._pipeline.wrap([$class: "MaskPasswordsBuildWrapper", 
            varPasswordPairs: [[password: this._credentials]]]) 
        {
            def response = this._pipeline.httpRequest customHeaders:  [
                [name: 'Authorization', value: "Basic ${ this._credentials }"]
            ], 
            url: "${ this._baseUrl }/crumbIssuer/api/json",
            validResponseCodes: "200:599"

            def status = response.status
            def content = response.content

            def output = getResponseMessage(status)
            if(output[0]) {
                def jsonContent = this._pipeline.readJSON text: content
                this._crumbs = jsonContent.crumb
            }

            return output << null
        }
    } 

    def testCredential(String credentialType, Map parameters) {
        def body = []
        parameters.each{k, v -> 
            body << [name: k, body: v.toString()]
        }

        def maskValues = [[password: this._credentials]]
        if(this._crumbs != null)
            maskValues << [password: this._crumbs]

        this._pipeline.wrap([$class: "MaskPasswordsBuildWrapper", 
            varPasswordPairs: maskValues]) 
        {
            def headers = [[ name: 'Authorization', value: "Basic ${ this._credentials }" ]]
            if(this._crumbs != null) 
                headers << [ name: 'Jenkins-Crumb', value: this._crumbs ]

            def response = this._pipeline.httpRequest customHeaders: headers, 
                url: "${ this._jobUrl }/descriptorByName/com.datapipe.jenkins.vault.credentials.common.${ credentialType }/testConnection",
                formData: body,
                httpMode: 'POST',
                validResponseCodes: "200"

            def status = response.status
            def content = response.content

            def isSuccess = content.contains('Tests were passed successfully') 
            if(!isSuccess)
                this._pipeline.echo "Jenkins API: Test credential FAIL. Text: ${ content }"

            return isSuccess
        }
    }

    def loadCredentials() {
        this._pipeline.wrap([$class: "MaskPasswordsBuildWrapper", 
            varPasswordPairs: [[password: this._credentials]]]) 
        {
            def response = this._pipeline.httpRequest customHeaders:  [
                    [name: 'Authorization', value: "Basic ${ this._credentials }"]
                ], 
                url: "${ this._jobUrl }/credentials/store/folder/domain/_/api/json?depth=1",
                validResponseCodes: "200:599"

            def status = response.status
            def content = response.content

            def output = getResponseMessage(status)
            if(output[0]) {
                def jsonContent = this._pipeline.readJSON text: content
                this._credentialsPool = jsonContent.credentials
            }

            return output << null
        }
    }

    def getCredentials() {
        if(this._credentialsPool == null) 
            error "Jenkins API: Use \"loadCredentials\" before use \"getCredentialsPool\""

        return this._credentialsPool
    }

    def createOrUpdateCredential(String credentialName, String xmlData) {
        if(this._credentialsPool == null) 
            error "Jenkins API: Use \"loadCredentials\" before use \"createOrUpdateCredential\""

        if(this._crumbs == null) {
            this._pipeline.echo "Jenkins API (createOrUpdateCredential): Crubms not loaded. Try without them"
        }
        
        this._pipeline.wrap([$class: "MaskPasswordsBuildWrapper", 
            varPasswordPairs: [[ password: this._credentials ], [ password: this._crumbs ]]]) 
        {
            def urlPart = this.isCredentialExists(credentialName) ? "credential/${ credentialName }/config.xml" : "createCredentials"
            def crumbsPart = this._crumbs == null ? "" : "-H \"Jenkins-Crumb: ${ this._crumbs }\""
            def checkUrl = "curl -k -skL -w '%{http_code}' \
                    -X POST \
                    -H \"Content-Type: application/xml\" \
                    -H \"Authorization: Basic ${ this._credentials }\" ${ crumbsPart } \
                    --url \"${ this._jobUrl }/credentials/store/folder/domain/_/${ urlPart }\"  -o /dev/null -d @- << EOF\n${ xmlData }\nEOF".stripIndent()
            
            def response = this._pipeline.sh(script: checkUrl, returnStdout: true)
            def status = response.toInteger()

            def output = getResponseMessage(status)
            return output << null
        }
    }

    def isCredentialExists(String credentialName) {
        def result = this._credentialsPool.find { it.id == credentialName } != null
        return result
    }

    def createCredential(String credentialName, String xmlData) {
        return this.createOrUpdateCredential(credentialName, xmlData)    
    }

    def updateCredential(String credentialName, String xmlData) {
        return this.createOrUpdateCredential(credentialName, xmlData)    
    }

    def getResponseMessage(Integer status) {
        def successStatuses = [ 200, 204 ]
        def response = [ successStatuses.contains(status), status ]
        switch (status) {
            case 200: 
            case 204: 
                response << null
                break
            case 400: 
                reponse << "Invalid request, missing or invalid data"
                break
            case 401:
                response << "Invalid credentials"
                break
            case 404:
                response << "Invalid path"
                break
            case 500: 
                response << "Internal server error"
                break
            default:
                response << 'Unknows status code'
                break
        }

        return response
    }

    def createOrUpdateJob(stage, jobName, xmlPath) {
        def url = ""
        if(stage == 'update')
            url = "${ this._jobUrl }/job/${ jobName }/config.xml" 
        else if (stage == 'create') 
            url = "${ this._jobUrl }/createItem?name=${ jobName }"
        else 
            this._pipeline.error "Unknown stage URL"

        def contentTypes = [
            "-H \"Content-Type: application/xml; charset=utf-8\"",
            "-H \"Content-Type: application/octet-stream; charset=utf-8\""
        ]

        def isSuccessUpload = false
        this._pipeline.wrap([$class: "MaskPasswordsBuildWrapper", 
            varPasswordPairs: [[ password: this._credentials ], [ password: this._crumbs ]]]) 
        {
            def crumbsPart = this._crumbs == null ? "" : "-H \"Jenkins-Crumb: ${ this._crumbs }\""
            contentTypes.each { contentType -> 
                if(isSuccessUpload)
                    return

                def checkUrl = "curl -k -skL -w '%{http_code}' -H \"Authorization: Basic ${ this._credentials }\" -X POST ${ contentType } ${ crumbsPart } --url \"${ url }\" --data-binary \"@${ xmlPath }\" -o /dev/null"  
                def response = this._pipeline.sh(script: checkUrl, returnStdout: true)
                
                isSuccessUpload = response == '200'
            }
        }

        return isSuccessUpload
    }
}

def init(pipeline, String buildUrl, String login, String password) {
    return new Jenkins(pipeline, buildUrl, login, password)
}

return this

