## Overview
Using [PowSyBl](https://www.powsybl.org/):
- Import CGMES 2.4.15 test configuration files using [PowSyBl's CGMES importer](https://www.powsybl.org/pages/documentation/grid/formats/cim-cgmes.html)
- Runs PowSyBl's [OpenLoadFlow](https://www.powsybl.org/pages/documentation/simulation/powerflow/openlf.html)
- Export grid models in [PowSyBl's XIIDM format](https://www.powsybl.org/pages/documentation/grid/formats/xiidm.html) 
- Produce [Diagrams](https://www.powsybl.org/pages/documentation/developer/repositories/powsybl-diagram.html) (Area and single line)

## Quick start
Download ENTSO-E CGMES CAS v2.0 TestConfigurations_packageCASv2.0.zip from [here](https://www.entsoe.eu/data/cim/cim-conformity-and-interoperability/#conformity-assessment-scheme).  
And extract this zip as a folder at the root of this repository.

```
TestConfigurations_packageCASv2.0/
├── MicroGrid
├── RealGrid
└── ...

```

Then run
```
mvn clean test
```

Results are produced in `output` folder:
```
output/
├── microGridBC
├── microGridT1
├── microGridT2
├── microGridT4
└── realGrid
```