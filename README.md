# Setting Up Jenkins CI/CD Pipeline with Kubernetes

The project is a complete DevOps pipeline designed to automate and optimize the software development lifecycle, from code integration to deployment and monitoring. It incorporates cutting-edge tools and technologies to implement robust Continuous Integration (CI) and Continuous Deployment (CD) processes. GitHub serves as the source code repository, while Jenkins orchestrates the CI/CD workflows. Maven is using for build & test code where Code quality and security are ensured using SonarQube, while Docker agent manages containerization. ArgoCD handles deployment to Kubernetes, enabling efficient application delivery to production environments.

![CI/CD Pipeline Architecture](https://user-images.githubusercontent.com/43399466/228301952-abc02ca2-9942-4a67-8293-f76647b6f9d8.png)

Step1 : Create a jenkins machine using terraform script
Create 1 Master machine on AWS with 2CPU, 8GB of RAM (t2.large) and 25 GB of storage & Open the below ports in security group of master machine and also attach same security group to Jenkins worker node,

[![1Capture.png](https://i.postimg.cc/GhgXVJ1Q/1Capture.png)](https://postimg.cc/zVWwRhvb)

**Note: ** By default, Jenkins will not be accessible to the external world due to the inbound traffic restriction by AWS. Open port 8080 in the inbound traffic rules as show below.

- EC2 > Instances > Click on <Instance-ID>
- In the bottom tabs -> Click on Security
- Security groups
- Add inbound traffic rules as shown in the image
- 
[![2Capture.png](https://i.postimg.cc/sD1F04Lj/2Capture.png)](https://postimg.cc/ykqrDcBt)

### Install Jenkins.

Pre-Requisites:
 - Java (JDK)

### Run the below commands to install Java and Jenkins

Install Java

```
sudo apt update
sudo apt install openjdk-17-jre
```

Verify Java is Installed

```
java -version
```

Now, you can proceed with installing Jenkins

```
curl -fsSL https://pkg.jenkins.io/debian/jenkins.io-2023.key | sudo tee \
  /usr/share/keyrings/jenkins-keyring.asc > /dev/null
echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] \
  https://pkg.jenkins.io/debian binary/ | sudo tee \
  /etc/apt/sources.list.d/jenkins.list > /dev/null
sudo apt-get update
sudo apt-get install jenkins
```
[![869-CA117-D26-B-478-B-A206-68489-BD02256.png](https://i.postimg.cc/FHY8FFb0/869-CA117-D26-B-478-B-A206-68489-BD02256.png)](https://postimg.cc/LJMNNpbs)
## Docker Slave Configuration

Run the below command to Install Docker in Ubuntu Machine

```
sudo apt update
sudo apt install docker.io
```
[![B70788-AD-608-F-4-C84-9790-4-ED32-AB0-DB20.png](https://i.postimg.cc/4xMCpH48/B70788-AD-608-F-4-C84-9790-4-ED32-AB0-DB20.png)](https://postimg.cc/qN8ZTR3n)

### Grant Jenkins user and Ubuntu user permission to docker deamon.

```
sudo su - 
usermod -aG docker jenkins
usermod -aG docker ubuntu
systemctl restart docker
```

Once you are done with the above steps, it is better to restart Jenkins.

```
http://http://3.109.217.192:8080/restart
```

### The Docker way

Build the Docker Image

```
docker build -t ultimate-cicd-pipeline:v1 .
```

```
docker run -d -p 8010:8080 -t ultimate-cicd-pipeline:v1
```

Access the application on `http://3.109.217.192:8010` 

### Configure a Sonar Server locally

```
apt install unzip
adduser sonarqube
wget https://binaries.sonarsource.com/Distribution/sonarqube/sonarqube-9.4.0.54424.zip
unzip *
chmod -R 755 /home/sonarqube/sonarqube-9.4.0.54424
chown -R sonarqube:sonarqube /home/sonarqube/sonarqube-9.4.0.54424
cd sonarqube-9.4.0.54424/bin/linux-x86-64/
./sonar.sh start
```
[![AD642449-6604-48-E4-9-B6-E-E90-D7-CA12534.png](https://i.postimg.cc/fRWXYkq0/AD642449-6604-48-E4-9-B6-E-E90-D7-CA12534.png)](https://postimg.cc/qzYzTBTk)
## Install Required Jenkins Plugins

Install the following plugins from Jenkins **Dashboard > Manage Jenkins > Plugins:**

1. Git Plugin
2. Maven Integration Plugin
3. Stage View Pipeline Plugin
4. Kubernetes Continuous Deploy (ArgoCD) Plugin
5. Docker Pipeline Plugin
6. SonarQube Scanner Plugin
[![image.png](https://i.postimg.cc/vT6xHVmp/image.png)](https://postimg.cc/BP4nmtHg)

1. Add Github credentials to push updated code from the pipeline:
2.  Add Sonarqube credentials for code scaning & vulnerability checking
3.  Add credentials for docker login to push docker image

[![image.png](https://i.postimg.cc/kX4hwMpL/image.png)](https://postimg.cc/c6pmLWZc)

## Continuous Integration (CI) Pipeline: Create Jenkins Pipeline Job

1. Go to Jenkins Dashboard
2. Click "New Item"
3. Enter job name (e.g., "CICD pipeline")
4. Select "Pipeline"
5. Click "OK"
[![image.png](https://i.postimg.cc/2yKpT1F8/image.png)](https://postimg.cc/tnWm4CML)

## Configure Pipeline Stages (jenkinsfile)

Here's a complete Jenkinsfile with all stages:
```groovy
pipeline {
  agent {
    docker {
      image 'maven:3.8.1-openjdk-11'
      args '--user root -v /var/run/docker.sock:/var/run/docker.sock' // mount Docker socket to access the host's Docker daemon
    }
  }
  stages {
    stage('Checkout') {
      steps {
        sh 'echo passed'
      }
    }
    stage('Build and Test') {
      steps {
        sh 'ls -ltr'
        // build the project and create a JAR file
        sh 'cd "Java based application/Java-spring-boot-Application" && mvn clean package'
      }
    }
    stage('Static Code Analysis') {
      environment {
        SONAR_URL = "http://3.109.217.192:9000"
      }
      steps {
        withCredentials([string(credentialsId: 'sonarqube', variable: 'SONAR_AUTH_TOKEN')]) {
          sh 'cd "Java based application/Java-spring-boot-Application" && mvn sonar:sonar -Dsonar.login=$SONAR_AUTH_TOKEN -Dsonar.host.url=${SONAR_URL}'
        }
      }
    }
    stage('Build and Push Docker Image') {
      environment {
        DOCKER_IMAGE = "rakibhasan031/ultimate-cicd:${BUILD_NUMBER}"
        // DOCKERFILE_LOCATION = "Java based application/Java-spring-boot-Applicatio/Dockerfile"
        REGISTRY_CREDENTIALS = credentials('docker-cred')
      }
      steps {
        script {
            sh '''
              apt-get update && apt-get install -y docker.io
              cd "Java based application/Java-spring-boot-Application" && docker build -t ${DOCKER_IMAGE} .
            '''
            def dockerImage = docker.image("${DOCKER_IMAGE}")
            docker.withRegistry('https://index.docker.io/v1/', "docker-cred") {
                dockerImage.push()
            }
        }
      }
    }
    stage('Update Deployment File') {
        environment {
            GIT_REPO_NAME = "Implementation-of-Jenkins-Pipeline"
            GIT_USER_NAME = "Rakib-Hasan031"
        }
        steps {
            withCredentials([string(credentialsId: 'github', variable: 'GITHUB_TOKEN')]) {
                sh '''
                    git config user.email "hasanrakib373@gmail.com"
                    git config user.name "Rakib Hasan"
                    BUILD_NUMBER=${BUILD_NUMBER}
                    sed -i "s/replaceImageTag/${BUILD_NUMBER}/g" "Java based application/Mainfest-Repo/deployment.yml"
                    git add -A
                    git commit -m "Update deployment image to version ${BUILD_NUMBER}"
                    git push https://${GITHUB_TOKEN}@github.com/${GIT_USER_NAME}/${GIT_REPO_NAME} HEAD:main
                '''
            }
        }
    }
  }
}

```

This Jenkins pipeline automates building, testing, static code analysis, Docker image creation, and deployment file update for a Java Spring Boot Application.
📌 Pipeline Stages Overview
1️⃣ Checkout Code

📜 Description: This stage checks out the project source code from the repository.


2️⃣ Build and Test

📜 Description:

    1. Lists files to verify directory structure.
    2. Compiles the Java project using Maven.
    3. Generates the JAR file for deployment.

[![image.png](https://i.postimg.cc/c4zgtT3P/image.png)](https://postimg.cc/Xpw7TfQg)

3️⃣ Static Code Analysis (SonarQube)

📜 Description:

    1. Runs SonarQube analysis to check code quality, security vulnerabilities, and code smells.
    2. Uses SonarQube credentials and the SonarQube server URL.
[![image.png](https://i.postimg.cc/5292NRnw/image.png)](https://postimg.cc/56GVsnc0)
[![image.png](https://i.postimg.cc/wxn362mv/image.png)](https://postimg.cc/fkjMCx5n)

4️⃣ Build and Push Docker Image

📜 Description:

    1. Installs Docker if not already available.
    2. Builds a Docker image for the Java application.
    3. Pushes the image to Docker Hub for deployment.
    
[![image.png](https://i.postimg.cc/jjghDys5/image.png)](https://postimg.cc/Z9NNgBvt)

5️⃣ Update Deployment File

📜 Description:

    1. Updates the Kubernetes deployment YAML file with the new Docker image tag.
    2. Commits the changes to GitHub.

[![image.png](https://i.postimg.cc/VvBzzqtG/image.png)](https://postimg.cc/RJFkRnXK)

## Step 4: Set Up Required Tools

### Maven Setup
```bash
# Install Maven
sudo apt-get update
sudo apt-get install maven

# Verify installation
mvn --version
```

### Docker Setup
```bash
# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Add Jenkins user to docker group
sudo usermod -aG docker jenkins
```

### Kubernetes Setup
```bash
# Install kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# Install Helm
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
```

## CI Pipeline Output

[![image.png](https://i.postimg.cc/sfcFDD8N/image.png)](https://postimg.cc/dDLN5vJ2)

## Step 6: Set Up SonarQube

1. Start SonarQube server:
```bash
docker run -d --name sonarqube -p 9000:9000 sonarqube:latest
```

2. Configure SonarQube in Jenkins:
   - Install SonarQube Scanner plugin
   - Add SonarQube server details in Jenkins configuration
   - Add SonarQube webhook for quality gate checks

## Step 7: Configure Argo CD

1. Install Argo CD:
```bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

2. Access Argo CD UI:
```bash
kubectl port-forward svc/argocd-server -n argocd 8080:443
```

3. Get initial admin password:
```bash
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
```

## Step 8: Run the Pipeline

1. Go to your pipeline in Jenkins
2. Click "Build Now"
3. Monitor the pipeline execution
4. Check logs for any errors

## Troubleshooting

Common issues and solutions:

1. Docker permission denied
```bash
sudo chmod 666 /var/run/docker.sock
```

2. Kubernetes connection issues
```bash
# Verify kubeconfig
kubectl config view
```

3. Maven build failures
```bash
# Clean Maven cache
rm -rf ~/.m2/repository
```

## Security Considerations

1. Always use Jenkins credentials for sensitive data
2. Implement RBAC in Kubernetes
3. Regularly update dependencies
4. Scan Docker images for vulnerabilities
5. Use network policies in Kubernetes

## Support

For issues and questions:
1. Create an issue in the repository
2. Contact the maintainers
3. Check Jenkins and Kubernetes logs

---
Created by Rakib - DevOps Enthusiast
