#!/bin/python

import os
import xml.etree.ElementTree as ET
from xml.dom import minidom

res_path: str = "composeApp/src/androidMain/res"


def fix_quot_and_header():
    for root, dirs, files in os.walk(res_path):
        if "values" in os.path.basename(root):
            file_path = os.path.join(root, "strings.xml")
            if os.path.exists(file_path):
                tree = ET.parse(file_path)
                root_elem = tree.getroot()
                # Corrige les &quot; dans toutes les valeurs
                for string in root_elem.findall("string"):
                    if string.text:
                        string.text = string.text.replace('&quot;', '"')
                # Re-beautify .xml file (pour l'indentation)
                tree_as_str = ET.tostring(root_elem, encoding="utf-8")
                reparsed = minidom.parseString(tree_as_str)
                pretty_xml = reparsed.toprettyxml(indent="    ")
                pretty_xml = "\n".join([line for line in pretty_xml.splitlines() if line.strip()])  # Enlève les lignes vides
                # S'assure que l'en-tête est correct
                correct_header = '<?xml version="1.0" encoding="utf-8"?>'
                lines = pretty_xml.splitlines()
                if not lines[0].strip().startswith('<?xml') or lines[0].strip() != correct_header:
                    if lines[0].strip().startswith('<?xml'):
                        lines[0] = correct_header
                    else:
                        lines.insert(0, correct_header)
                pretty_xml = "\n".join(lines)
                with open(file_path, 'w', encoding="utf-8") as file:
                    file.write(pretty_xml)
                print(f"Fichier corrigé : {file_path}")

if __name__ == '__main__':
    fix_quot_and_header()



