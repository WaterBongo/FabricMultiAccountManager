from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.staticfiles import StaticFiles
from fastapi.responses import HTMLResponse
import asyncio
import json
import logging
from typing import Dict, Set, List

app = FastAPI()
app.mount("/static", StaticFiles(directory="static"), name="static")

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class ConnectionManager:
    def __init__(self):
        self.minecraft_clients: List[WebSocket] = []
        self.web_clients: List[WebSocket] = []
        self.client_info: Dict[WebSocket, dict] = {}

    async def connect_minecraft(self, websocket: WebSocket, client_info: dict):
        self.minecraft_clients.append(websocket)
        self.client_info[websocket] = client_info
        await self.broadcast_status()

    async def connect_web(self, websocket: WebSocket):
        await websocket.accept()
        self.web_clients.append(websocket)
        await self.broadcast_status()

    async def disconnect(self, websocket: WebSocket):
        if websocket in self.minecraft_clients:
            self.minecraft_clients.remove(websocket)
        if websocket in self.web_clients:
            self.web_clients.remove(websocket)
        if websocket in self.client_info:
            del self.client_info[websocket]
        await self.broadcast_status()


    async def send_to_targeted_minecraft_clients(self, message: str, targets: List[str]):
        for client in self.minecraft_clients.copy():
            try:
                client_name = self.client_info[client].get("name")
                if client_name in targets:
                    await client.send_text(message)
            except Exception as e:
                logger.error(f"Failed to send to minecraft client: {e}")
                await self.disconnect(client)



    async def broadcast_status(self):
        status = {
            "type": "status",
            "clients": [
                {
                    "name": info.get("name", "Unknown"),
                    "status": info.get("status", "Unknown"),
                    "ip": info.get("ip", "Unknown"),
                    "cords": {
                        "x": info.get("x", "Unknown"),
                        "y": info.get("y", "Unknown"),
                        "z": info.get("z", "Unknown"),
                    },
                    "health": info.get("health", "Unknown"),
                    "hunger": info.get("hunger", "Unknown"),
                    "dimension": info.get("dimension", "Unknown"),
                    
                }
                for info in self.client_info.values()
            ]
        }
        await self.broadcast_to_web(json.dumps(status))

    async def broadcast_to_minecraft(self, message: str):
        for client in self.minecraft_clients.copy():  # Use copy to avoid modification during iteration
            try:
                await client.send_text(message)
            except Exception as e:
                logger.error(f"Failed to send to minecraft client: {e}")
                await self.disconnect(client)

    async def broadcast_to_web(self, message: str):
        for client in self.web_clients.copy():  # Use copy to avoid modification during iteration
            try:
                await client.send_text(message)
            except Exception as e:
                logger.error(f"Failed to send to web client: {e}")
                await self.disconnect(client)

manager = ConnectionManager()

@app.get("/")
async def get():
    with open("static/index.html", encoding="utf-8") as f:
        return HTMLResponse(f.read())

@app.websocket("/ws/minecraft")
async def minecraft_websocket(websocket: WebSocket):
    try:
        await websocket.accept()
        
        # Wait for initial connection message
        connect_msg = await websocket.receive_text()
        client_info = json.loads(connect_msg)
        
        await manager.connect_minecraft(websocket, client_info)
        
        while True:
            try:
                message = await websocket.receive_text()
                data = json.loads(message)
                
                if data["type"] == "status_update":
                    if websocket in manager.client_info:
                        manager.client_info[websocket].update(data.get("status", {}))
                        await manager.broadcast_status()
                
                await manager.broadcast_to_web(message)
                
            except WebSocketDisconnect:
                break
            except Exception as e:
                logger.error(f"Error processing minecraft message: {e}")
                break
                
    except Exception as e:
        logger.error(f"Error in minecraft websocket: {e}")
    finally:
        await manager.disconnect(websocket)

@app.websocket("/ws/web")
async def web_websocket(websocket: WebSocket):
    try:
        await manager.connect_web(websocket)
        
        while True:
            try:
                message = await websocket.receive_text()
                data = json.loads(message)
                print(message)
                
                # Generic handling for all message types
                if "targets" in data and data["targets"]:
                    await manager.send_to_targeted_minecraft_clients(message, data["targets"])
                else:
                    # If no targets specified, broadcast to all
                    await manager.broadcast_to_minecraft(message)
                
            except WebSocketDisconnect:
                break
            except Exception as e:
                logger.error(f"Error processing web message: {e}")
                break
                
    except Exception as e:
        logger.error(f"Error in web websocket: {e}")
    finally:
        await manager.disconnect(websocket)
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)