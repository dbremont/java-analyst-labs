import http.server
import socketserver
import json
import time
import random

# Configuration
PORT = 8000
STEP_MS = 200  # Simulated time step per tick
REAL_SLEEP = 0.1 # Real time delay to simulate processing speed

# Data definitions
THREAD_GROUPS = [
    {'id': 'main', 'name': 'Main Process', 'color': '#2DD4BF', 'type': 'persistent'},
    {'id': 'net', 'name': 'Network I/O', 'color': '#818CF8', 'type': 'ephemeral'},
    {'id': 'db', 'name': 'Database Pool', 'color': '#FACC15', 'type': 'ephemeral'},
    {'id': 'worker', 'name': 'Worker Threads', 'color': '#FB7185', 'type': 'ephemeral'}
]

FUNC_NAMES = {
    'main': ['ProcessEvent', 'UpdateState', 'RenderFrame', 'ComputeLayout', 'GC Collect'],
    'net': ['HTTP Parse', 'TLS Handshake', 'Read Socket', 'Write Buffer', 'Compress'],
    'db': ['Query Plan', 'Index Scan', 'Fetch Rows', 'Commit', 'Rollback'],
    'worker': ['Hash Compute', 'Image Resize', 'Data Parse', 'Encrypt', 'Cleanup']
}

class ProfilingHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/':
            self.path = 'index.html'
            return http.server.SimpleHTTPRequestHandler.do_GET(self)
        elif self.path == '/stream':
            self.send_response(200)
            self.send_header('Content-type', 'text/event-stream')
            self.send_header('Cache-Control', 'no-cache')
            self.send_header('Connection', 'keep-alive')
            self.end_headers()
            self.run_infinite_stream()
        else:
            self.send_error(404)

    def run_infinite_stream(self):
        current_time = 0
        thread_id_counter = 0
        
        # Initial Main Thread setup
        # In an infinite stream, the 'main' thread usually persists
        main_thread = {
            'id': thread_id_counter, 
            'name': 'Main Process', 
            'groupId': 'main', 
            'start': 0, 
            # End time doesn't matter as much for persistent threads in live view, 
            # but we set it far future or update it dynamically
            'end': 999999999, 
            'color': '#2DD4BF'
        }
        
        init_payload = {
            'type': 'init',
            'threads': [main_thread],
            'zones': []
        }
        self.send_event(init_payload)
        thread_id_counter += 1

        try:
            while True:
                chunk_end = current_time + STEP_MS
                
                new_threads = []
                new_zones = []
                
                # 1. Generate zones for Main Thread (continuously running)
                t = current_time
                while t < chunk_end:
                    dur = 0.5 + random.random() * 2
                    zone_end = t + dur
                    new_zones.append({
                        'threadId': 0,
                        'name': random.choice(FUNC_NAMES['main']),
                        'start': round(t, 3),
                        'end': round(zone_end, 3),
                        'depth': random.randint(0, 2),
                        'color': '#2DD4BF'
                    })
                    t = zone_end + random.random() * 0.2

                # 2. Generate ephemeral threads
                for group in THREAD_GROUPS[1:]:
                    # Spawn chance
                    if random.random() > 0.5:
                        start = current_time + random.random() * STEP_MS * 0.8
                        dur = 1 + random.random() * 15
                        
                        thread = {
                            'id': thread_id_counter,
                            'name': f"{group['name']} #{thread_id_counter}",
                            'groupId': group['id'],
                            'start': round(start, 3),
                            'end': round(start + dur, 3),
                            'color': group['color']
                        }
                        new_threads.append(thread)
                        
                        # Zones for this thread
                        zt = thread['start']
                        funcs = FUNC_NAMES[group['id']]
                        while zt < thread['end']:
                            zDur = 0.2 + random.random() * 1.0
                            new_zones.append({
                                'threadId': thread_id_counter,
                                'name': random.choice(funcs),
                                'start': round(zt, 3),
                                'end': round(min(zt + zDur, thread['end']), 3),
                                'depth': 0,
                                'color': group['color']
                            })
                            zt += zDur * 1.1
                        
                        thread_id_counter += 1

                # Send the chunk
                payload = {
                    'type': 'chunk',
                    'threads': new_threads,
                    'zones': new_zones,
                    'time': chunk_end
                }
                self.send_event(payload)
                
                current_time = chunk_end
                time.sleep(REAL_SLEEP)

        except (BrokenPipeError, ConnectionResetError):
            print("Client disconnected.")
        except Exception as e:
            print(f"Error: {e}")

    def send_event(self, data):
        msg = f"data: {json.dumps(data)}\n\n"
        self.wfile.write(msg.encode('utf-8'))
        self.wfile.flush()

class ThreadedTCPServer(socketserver.ThreadingMixIn, socketserver.TCPServer):
    allow_reuse_address = True

if __name__ == "__main__":
    print(f"Server running at http://localhost:{PORT}")
    with ThreadedTCPServer(("", PORT), ProfilingHandler) as httpd:
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nServer stopped.")