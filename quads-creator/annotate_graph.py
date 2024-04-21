import argparse
import os
import hashlib
import rdflib
from rdflib import Dataset, URIRef, Literal, ConjunctiveGraph
from pathlib import Path

RDFLIB_INPUT_SUPPORTED_FORMATS = ['turtle', 'ttl', 'turtle2', 'xml',
                                  'pretty-xml', 'json-ld', 'ntriples', 'nt', 'nt11', 'n3', 'trig', 'trix']
ANNOTATION_TYPES = ['theoretical', 'relational']


def main():
    parser = argparse.ArgumentParser(description='Annotate RDF graph formats')
    parser.add_argument('output_folder', help='Specify the output folder')
    parser.add_argument('input_folder', help='Specify the input folder')
    parser.add_argument('input_file', help='Specify the input datafile, folder, or URL')
    parser.add_argument('input_format', choices=RDFLIB_INPUT_SUPPORTED_FORMATS,
                        help='Specify the input RDF format (only RDFLib supported formats)')
    parser.add_argument('annotation_type', choices=ANNOTATION_TYPES,
                        help='Specify the annotation type for the given triples')
    parser.add_argument('annotation', help='Specify the graph name annotation')
    args = parser.parse_args()

    if args.annotation is None:
        print('Graph name annotation is missing')
        exit(1)
    print(f'({args.annotation_type} annotation) - file: {args.input_file} with {args.annotation}')

    converter = RdfConverter(args)
    converter.convert(args.input_folder, args.input_file, args.input_format, args.output_folder)


class RdfConverter:
    def __init__(self, args):
        self.args = args
        self.filename = os.path.split(args.input_file)[-1]
        self.graph = rdflib.Graph()
        self.workspace_graph = ConjunctiveGraph()
        self.version = f'https://github.com/VCityTeam/SPARQL-to-SQL/Version#{self.filename}'
        self.annotation = f'https://github.com/VCityTeam/SPARQL-to-SQL/Named-Graph#{args.annotation}'

        if args.annotation_type == 'theoretical':
            self.graph_name = ('https://github.com/VCityTeam/SPARQL-to-SQL/Versioned-Named-Graph#'
                               + hashlib.sha256(
                                        (self.annotation + self.filename).encode("utf-8")
                                    ).hexdigest()
                               )
        else:
            self.graph_name = f'https://github.com/VCityTeam/SPARQL-to-SQL/Named-Graph#{args.annotation}'
        self.annotation_type = args.annotation_type

    def convert(self, input_folder, input_file, input_format, output_folder):
        """
        It takes an input file, an input format, an output file, and an annotation, and it adds annotation to the
        input file from the input format and saves it to the output file

        :param input_folder: The folder containing the input files
        :param input_file: The file to be converted
        :param input_format: The format of the input RDF file. Must be an RDFlib compliant format
        :param output_folder: The folder to write the output to
        """
        self.graph.parse(os.path.join(input_folder, input_file), format=input_format)
        Path(output_folder).mkdir(parents=True, exist_ok=True)

        ds = Dataset()
        named_graph = URIRef(self.graph_name)

        for s, p, o in self.graph.query('''
                SELECT ?s ?p ?o
                WHERE { ?s ?p ?o . }'''):
            # check if s, p and o are URI or Literal
            subject = self.create_uriref_or_literal(s)
            predicate = URIRef(p)
            obj = self.create_uriref_or_literal(o)

            # Ajouter le quadruplet au jeu de données
            ds.add((subject, predicate, obj, named_graph))
        if self.annotation_type == 'theoretical':
            self.add_theoretical_annotation(named_graph, input_folder, output_folder)
            ds.serialize(destination=output_folder + self.filename + '.theoretical.nq',
                         format='nquads', encoding='utf-8')
        else:
            ds.serialize(destination=output_folder + self.filename + '.relational.nq',
                         format='nquads', encoding='utf-8')

    def add_theoretical_annotation(self, named_graph, input_folder, output_folder):
        """
        Creates a new ttl file or append at the end the annotation for the named graph
        :param named_graph: The named graph to be annotated
        :param input_folder: The folder containing the input files
        :param output_folder: The folder to write the output to
        """
        workspace_ds = Dataset()
        workspace_uri = URIRef('https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Ontologies/Workspace/3.0/workspace')
        if os.path.exists(os.path.join(output_folder, 'theoretical_annotations.nq')):
            self.workspace_graph.parse(
                os.path.join(output_folder, 'theoretical_annotations.nq'), format='nquads')
            for t in self.workspace_graph:
                subject = self.create_uriref_or_literal(t[0])
                predicate = URIRef(t[1])
                obj = self.create_uriref_or_literal(t[2])
                workspace_ds.add((subject, predicate, obj, workspace_uri))
        workspace_ds.add(
            (
                named_graph,
                URIRef('https://github.com/VCityTeam/SPARQL-to-SQL#is-version-of'),
                URIRef(self.annotation),
                workspace_uri
            )
        )
        workspace_ds.add(
            (
                named_graph,
                URIRef('https://github.com/VCityTeam/SPARQL-to-SQL#is-in-version'),
                URIRef(self.version),
                workspace_uri
            )
        )
        workspace_ds.serialize(
            destination=output_folder + 'theoretical_annotations.nq', format='nquads', encoding='utf-8')

    def create_uriref_or_literal(self, str):
        """Create a URIRef with the same validation func used by URIRef
            or a Literal if str is not an URI
        """
        if isinstance(str, URIRef):
            return URIRef(str)
        elif isinstance(str, Literal):
            return Literal(str)


if __name__ == "__main__":
    main()
