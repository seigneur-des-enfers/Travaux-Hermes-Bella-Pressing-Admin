from http.server import BaseHTTPRequestHandler, HTTPServer

class RequestHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        content_length = int(self.headers.get('Content-Length', 0))
        post_data = self.rfile.read(content_length)
        print("===== CRASH LOG RECEIVED =====")
        print(post_data.decode('utf-8'))
        print("==============================")
        self.send_response(200)
        self.end_headers()

if __name__ == '__main__':
    server = HTTPServer(('0.0.0.0', 8080), RequestHandler)
    print("Starting server at port 8080")
    server.serve_forever()
