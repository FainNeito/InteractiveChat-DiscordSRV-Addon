"""Reject duplicate dependency coordinates in every checked-in Maven model."""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
NS = {"m": "http://maven.apache.org/POM/4.0.0"}


class MavenModelTest(unittest.TestCase):
    def test_dependency_coordinates_are_unique(self):
        models = [ROOT / "pom.xml", *sorted(ROOT.glob("*/pom.xml"))]
        self.assertEqual(46, len(models), "Check the complete declared reactor")
        for model in models:
            root = ET.parse(model).getroot()
            for dependencies in root.findall(".//m:dependencies", NS):
                seen = set()
                for dependency in dependencies.findall("m:dependency", NS):
                    def field(name, default=""):
                        return dependency.findtext(f"m:{name}", default, NS)
                    key = (field("groupId"), field("artifactId"),
                           field("type", "jar"), field("classifier"))
                    with self.subTest(model=model.relative_to(ROOT), coordinate=key):
                        self.assertNotIn(key, seen, "Duplicate Maven dependency")
                    seen.add(key)


if __name__ == "__main__":
    unittest.main()
