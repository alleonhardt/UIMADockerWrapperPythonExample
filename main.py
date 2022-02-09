from cassis import *
import http.server
import spacy
import socketserver

with open('typesystem.xml', 'rb') as f:
    typesystem = load_typesystem(f)
    nlp = spacy.load('en_core_web_sm')

    class MyHttpRequestHandler(http.server.SimpleHTTPRequestHandler):
        def do_POST(self):
            content_len = int(self.headers.get('Content-Length'))
            post_body = self.rfile.read(content_len).decode("utf-8")
            cas = load_cas_from_xmi(post_body, typesystem=typesystem)


            # Sending an '200 OK' response
            self.send_response(200)

            # Setting the header
            self.send_header("Content-type", "text/xml")

            # Whenever using 'send_header', you also have to call 'end_headers'
            self.end_headers()

            Sentence = typesystem.get_type("de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence")
            text_sentences = nlp(cas.sofa_string)
            for sentence in text_sentences.sents:
                cas.add(Sentence(begin=sentence.start,end=sentence.end))
            xmi = cas.to_xmi()
            if xmi != None:
                self.wfile.write(xmi.encode('utf-8'))
            else:
                print("Error could not send data")
            return

    # Create an object of the above class
    handler_object = MyHttpRequestHandler

    PORT = 9714
    my_server = socketserver.TCPServer(("0.0.0.0", PORT), handler_object)

    print("Server started on port 9714")
    print("Server started on port 9714\r\n")
    # Star the server
    my_server.serve_forever()
