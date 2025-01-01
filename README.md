# Minecraft Multi-Account Controller

A comprehensive solution for controlling multiple Minecraft accounts through a web interface. This project combines a FastAPI backend server, an intuitive web dashboard, and a Fabric mod client to enable seamless multi-account management.

## Features Demonstration

### Connection Process
![Connection Process](assets/connection.gif)

<div align="center">
  <p><i>Seamless connection between Minecraft clients and the control center</i></p>
</div>

### Live Status Updates
![Live Info Updates](assets/LiveInfoUpdate.gif)
<div align="center">
  <p><i>Real-time status updates showing player health, position, and dimension</i></p>
</div>

### Multi-Account Management
![Multi Account Control](assets/MultiAccountControl.gif)
<div align="center">
  <p><i>Control multiple Minecraft accounts simultaneously</i></p>
</div>

### Single Account Control
![Single Account Control](assets/SingleAccountControl.gif)
<div align="center">
  <p><i>Precise control over individual accounts</i></p>
</div>

### Travel Functionality
![Travel Function](assets/TravelFunction.gif)
<div align="center">
  <p><i>Coordinate-based travel system for precise player movement</i></p>
</div>

### Web Interface
![GUI Overview](assets/gui.png)
<div align="center">
  <p><i>Clean and intuitive web dashboard for account management</i></p>
</div>

## Features
- Real-time monitoring of multiple Minecraft accounts
- Live status updates including:
  - Player position (X, Y, Z coordinates)
  - Health and hunger levels
  - Current dimension
- Command execution across multiple selected accounts
- Chat message broadcasting
- Location targeting system
- Quick command presets
- Visual account selection interface

## Technical Stack
<div align="center">
  <img src="https://img.shields.io/badge/Python-FastAPI-009688?style=for-the-badge&logo=fastapi"/>
  <img src="https://img.shields.io/badge/WebSocket-Protocol-4A90E2?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/Minecraft-Fabric_Mod-62B47A?style=for-the-badge&logo=minecraft"/>
  <img src="https://img.shields.io/badge/Java-Development-007396?style=for-the-badge&logo=java"/>
</div>

## Setup

### Prerequisites
- Python 3.7+
- Fabric Loader for Minecraft
- Java Development Kit (JDK)

### Installation

1. **Server Setup**
```bash
pip install fastapi uvicorn websockets
```

2. **Minecraft Mod Installation**
- Place the mod JAR file in your Minecraft mods folder
- Ensure Fabric Loader is installed

3. **Starting the Server**
```bash
python main.py
```

4. **Accessing the Web Interface**
- Open a web browser and navigate to `http://localhost:8000`

## Development Journey
This project is used to display,
- Full-stack development capabilities
- Real-time web socket implementation
- Worker/Host infrastructure design
- Multiple asynchronous connections
- User interface design
- System architecture planning

## Technical Challenges & Solutions

### Real-Time Communication
- Implemented WebSocket protocol for instant updates
- Solved connection stability issues with automatic reconnection
- Managed concurrent connections from multiple Minecraft clients

### Minecraft Integration
- Developed custom Fabric mod for game interaction
- Created reliable command execution system
- Implemented accurate player status tracking
- Synchronized game state with web interface

### User Interface Design
- Built responsive dashboard for real-time monitoring
- Designed intuitive multi-account selection system
- Created visual feedback for command execution
- Implemented dynamic status cards with live updates

### Backend Architecture
- Structured efficient WebSocket message routing
- Developed robust error handling system
- Created scalable client management system
- Implemented clean command processing pipeline



## Skills Demonstrated
- Backend Development (Python/FastAPI)
- Frontend Development (HTML/CSS/JavaScript)
- WebSocket Programming / Networking
- Java Development (Minecraft Modding)
- Real-time System Design
- User Interface Design
- Technical Documentation

## Contact
Edward He - Waterbongo.me