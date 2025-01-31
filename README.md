# Setting Up Jenkins CI/CD Pipeline with Kubernetes

The project is a complete DevOps pipeline designed to automate and optimize the software development lifecycle, from code integration to deployment and monitoring. It incorporates cutting-edge tools and technologies to implement robust Continuous Integration (CI) and Continuous Deployment (CD) processes. GitHub serves as the source code repository, while Jenkins orchestrates the CI/CD workflows. Code quality and security are ensured using SonarQube and Trivy, respectively, while Docker manages containerization. ArgoCD handles deployment to Kubernetes, enabling efficient application delivery to production environments.

![CI/CD Pipeline Architecture](https://user-images.githubusercontent.com/43399466/228301952-abc02ca2-9942-4a67-8293-f76647b6f9d8.png)

Step1 : Create a jenkins machine using terraform script
Create 1 Master machine on AWS with 2CPU, 8GB of RAM (t2.large) and 25 GB of storage & Open the below ports in security group of master machine and also attach same security group to Jenkins worker node,
[![1Capture.png](https://i.postimg.cc/GhgXVJ1Q/1Capture.png)](https://postimg.cc/zVWwRhvb)

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
http://<ec2-instance-public-ip>:8080/restart
```

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

## Install Required Jenkins Plugins

Install the following plugins from Jenkins **Dashboard > Manage Jenkins > Plugins:**

1. Git Plugin
2. Maven Integration Plugin
3. Pipeline Plugin
4. Kubernetes Continuous Deploy (ArgoCD) Plugin
5. Docker Pipeline Plugin
6. SonarQube Scanner Plugin
[![image.png](https://i.postimg.cc/kX4hwMpL/image.png)](https://postimg.cc/c6pmLWZc)
[![image.png](https://i.postimg.cc/vT6xHVmp/image.png)](https://postimg.cc/BP4nmtHg)
[![image.png](https://i.postimg.cc/jjghDys5/image.png)](https://postimg.cc/Z9NNgBvt)
## Step 2: Create Jenkins Pipeline Job

The docker agent configuration is now successful.

**Note: ** By default, Jenkins will not be accessible to the external world due to the inbound traffic restriction by AWS. Open port 8080 in the inbound traffic rules as show below.

- EC2 > Instances > Click on <Instance-ID>
- In the bottom tabs -> Click on Security
- Security groups
- Add inbound traffic rules as shown in the image (you can just allow TCP 8080 as well, in my case, I allowed `All traffic`).

1. Go to Jenkins Dashboard
2. Click "New Item"
3. Enter job name (e.g., "java-cicd-pipeline")
4. Select "Pipeline"
5. Click "OK"

## Step 3: Configure Pipeline Stages (jenkinsfile)

Here's a complete Jenkinsfile with all stages:

```groovy
pipeline {
    agent any
    
    environment {
        DOCKER_IMAGE = "your-docker-repo/java-app:${BUILD_NUMBER}"
        KUBECONFIG = credentials('kubernetes-config')
    }
    
    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'YOUR_REPO_URL'
            }
        }
        
        stage('Build') {
            steps {
                sh 'mvn clean package'
            }
        }
        
        stage('Unit Tests') {
            steps {
                sh 'mvn test'
            }
        }
        
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn sonar:sonar'
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    docker.build(DOCKER_IMAGE)
                }
            }
        }
        
        stage('Push Docker Image') {
            steps {
                script {
                    docker.withRegistry('https://registry.hub.docker.com', 'docker-credentials') {
                        docker.image(DOCKER_IMAGE).push()
                    }
                }
            }
        }
        
        stage('Deploy to Test') {
            steps {
                sh "helm upgrade --install java-app ./helm/java-app --namespace test"
            }
        }
        
        stage('Integration Tests') {
            steps {
                sh 'mvn verify -Pintegration-tests'
            }
        }
        
        stage('Deploy to Production') {
            steps {
                sh "argocd app create java-app --repo YOUR_REPO_URL --path helm/java-app --dest-server https://kubernetes.default.svc --dest-namespace production"
            }
        }
    }
    
    post {
        always {
            junit '**/target/surefire-reports/*.xml'
            cleanWs()
        }
    }
}
```

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

## Step 5: Configure Jenkins Credentials

Add the following credentials in Jenkins (Dashboard > Manage Jenkins > Credentials):

1. Git credentials (if using private repository)
2. Docker registry credentials
3. Kubernetes configuration file
4. SonarQube token

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
