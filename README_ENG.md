# PoC-Deployer-System

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

An Android privilege escalation application based on CVE-2024-31317, integrated with Termux terminal emulator features.

## Project Statement

### Acknowledgements
Although PoC-Deployer-System is now an independent project, special thanks to the original project for inspiration and contributions:

**Original Project**: https://github.com/Webldix/CVE-2024-31317-PoC-Deployer

## Update Log

### Latest Features
- **Custom injection of uid/gid/SELinux context/groups**
- **Added app data extraction feature**
- **Added Zygote log monitoring feature**
- **Support for Zygote parameters**

## Feature Showcase

### uid/gid/groups Injection Feature
<img src="https://raw.githubusercontent.com/wqry085/PoC-Deployer-System/main/jpg/a1.jpg" alt="应用图片" width="200" />

### Advanced Feature Interface
<img src="https://raw.githubusercontent.com/wqry085/PoC-Deployer-System/main/jpg/a2.jpg" alt="应用图片2" width="200" />

### Reverse Shell
<img src="https://raw.githubusercontent.com/wqry085/PoC-Deployer-System/main/jpg/a3.jpg" alt="应用图片3" width="200" />

## System Requirements

### Supported
- **Security Patch**: Before June 2024
- **Android Version**: 9 - 13
- **Permission Required**: Shizuku permission

## Main Features

### Core Functions
- **Simplified Implementation**: CVE-2024-31317 exploitation is simplified; users only need to grant Shizuku permission to perform all privilege escalation operations within the app
- **Termux Integration**: Includes some Termux terminal emulator features
- **Reverse Shell**: Supports reverse interactive shell
- **Permission Injection**: Full capability for custom injection of uid/gid/SELinux context/groups
- **Data Extraction**: App data extraction feature
- **Zygote Monitoring**: Zygote log monitoring and parameter configuration

### Technical Highlights
- Privilege escalation can be achieved with only Shizuku permission
- Complete operation interface
- Real-time system monitoring

## Usage Instructions

1. Install the app and grant Shizuku permission
2. Start socket listening
3. Configure the parameters to inject
4. Execute the injection

## Disclaimer

This project is for security research and educational purposes only. Do not use for illegal activities. Users must comply with local laws and regulations and are responsible for any consequences resulting from use of this project.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

**Contact Information:**
- GitHub: [wqry085](https://github.com/wqry085)

*Please read the license disclaimer and special warnings carefully before use.*

