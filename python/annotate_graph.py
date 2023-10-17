import argparse
import os

import rdflib
from rdflib import Dataset, URIRef, Literal


def main():
    RDFLIB_INPUT_SUPPORTED_FORMATS = ['turtle', 'ttl', 'turtle2', 'xml',
                                      'pretty-xml', 'json-ld', 'ntriples', 'nt', 'nt11', 'n3', 'trig', 'trix']
    ANNOTATION_TYPES = ['version', 'named_graph']
    parser = argparse.ArgumentParser(description='Annotate RDF graph formats')
    parser.add_argument('input_file', help='Specify the input datafile, folder, or URL')
    parser.add_argument('input_format', choices=RDFLIB_INPUT_SUPPORTED_FORMATS,
                        help='Specify the input RDF format (only RDFLib supported formats)')
    parser.add_argument('output_file', default='', help='Specify the output datafile')
    parser.add_argument('annotation_type', choices=ANNOTATION_TYPES,
                        help='Specify the annotation type for the given triples')
    parser.add_argument('annotation', nargs='?', help='Specify the annotation when the annotation type is "graph name"')
    args = parser.parse_args()

    if args.annotation_type == 'version':
        print(f'version annotation to input file: {args.input_file}')
    else:
        if args.annotation is None:
            print('Graph name annotation is missing')
            exit(1)
        print(f'named_graph annotation to input file: {args.input_file}')

    converter = RdfConverter(args)
    converter.convert(args.input_file, args.input_format, args.output_file)


class RdfConverter:
    def __init__(self, args):
        self.args = args
        self.filename = '.'.join(os.path.split(
            args.input_file)[-1].split('.')[:-1])
        self.graph = rdflib.Graph()
        self.graph_name = f'https://github.com/VCityTeam/SPARQL-to-SQL/Version#{self.filename}' \
            if args.annotation_type == 'version' \
            else f'https://github.com/VCityTeam/SPARQL-to-SQL/GraphName#{args.annotation}'
        self.annotation_type = args.annotation_type

    def convert(self, input_file, input_format, output_file):
        """
        It takes an input file, an input format, an output file, an annotation_type, and an annotation, and it adds annotation to the
        input file from the input format and saves it to the output file

        :param input_file: The file to be converted
        :param input_format: The format of the input RDF file. Must be an RDFlib compliant format
        :param output_file: The file to write the output to
        """
        self.graph.parse(input_file, format=input_format)
        ds = Dataset()
        named_graph = URIRef(self.graph_name)

        for s, p, o in self.graph.query('''
                SELECT ?s ?p ?o
                WHERE { ?s ?p ?o . }'''):
            # Définir un triple
            subject = URIRef(s)
            predicate = URIRef(p)
            object_literal = Literal(o)

            # Ajouter le quadruplet au jeu de données
            ds.add((subject, predicate, object_literal, named_graph))

        if self.annotation_type == 'version':
            ds.add(
                (
                    named_graph,
                    URIRef('http://www.w3.org/1999/02/22-rdf-syntax-ns#type'),
                    Literal('https://github.com/VCityTeam/SPARQL-to-SQL/GraphType#Version')
                )
            )
        else:
            ds.add(
                (
                    named_graph,
                    URIRef('http://www.w3.org/1999/02/22-rdf-syntax-ns#type'),
                    Literal('https://github.com/VCityTeam/SPARQL-to-SQL/GraphType#GraphName')
                )
            )
        ds.serialize(destination=output_file, format='nquads', encoding='utf-8')


if __name__ == "__main__":
    main()
