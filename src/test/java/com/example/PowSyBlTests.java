package com.example;

import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.nad.NetworkAreaDiagram;
import com.powsybl.nad.layout.LayoutParameters;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.openloadflow.network.SlackBusSelectionMode;
import com.powsybl.sld.SingleLineDiagram;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.*;

class PowSyBlTests {

    private static final Path outDir = Path.of("output");
    private final Path testConfiguration = Path.of("TestConfigurations_packageCASv2.0");
    private final Path microGrid = testConfiguration.resolve("MicroGrid");
    private final Path microGridBC = microGrid.resolve("BaseCase_BC");
    private final Path microGridT1 = microGrid.resolve("Type1_T1");
    private final Path microGridT2 = microGrid.resolve("Type2_T2");
    private final Path microGridT4 = microGrid.resolve("Type4_T4");
    private final Path microGridBCAssembled = microGridBC.resolve("CGMES_v2.4.15_MicroGridTestConfiguration_BC_Assembled_v2.zip");
    private final Path microGridT1Assembled = microGridT1.resolve("CGMES_v2.4.15_MicroGridTestConfiguration_T1_Assembled_Complete_v2.zip");
    private final Path microGridT2Assembled = microGridT2.resolve("CGMES_v2.4.15_MicroGridTestConfiguration_T2_Assembled_Complete_v2.zip");
    private final Path microGridT4BBAssembled = microGridT4.resolve("CGMES_v2.4.15_MicroGridTestConfiguration_T4_Assembled_BB_Complete_v2.zip");
    private final Path realGrid = testConfiguration.resolve("RealGrid").resolve("CGMES_v2.4.15_RealGridTestConfiguration_v2.zip");

    @Test
    void microGridBC() {
        final var name = "microGridBC";
        var network = readCgmesZip(microGridBCAssembled, true);
        assertThat(network.getSubstationCount()).isEqualTo(9);
        writeIidm(network, name, "init");
        var lfResult = LoadFlow.run(network, buildLoadFlowParameters());
        assertThat(lfResult.isOk()).isTrue();
        writeIidm(network, name, "solved");
        printSLDs(network, name);
    }

    @Test
    void microGridT1() {
        final var name = "microGridT1";
        var network = readCgmesZip(microGridT1Assembled);
        assertThat(network.getSubstationCount()).isEqualTo(3);
        writeIidm(network, name, "init");
        var lfResult = LoadFlow.run(network, buildLoadFlowParameters());
        assertThat(lfResult.isOk()).isTrue();
        writeIidm(network, name, "solved");
        printSLDs(network, name);
    }

    @Test
    void microGridT2() {
        final var name = "microGridT2";
        var network = readCgmesZip(microGridT2Assembled);
        assertThat(network.getSubstationCount()).isEqualTo(7);
        writeIidm(network, name, "init");

        var lfResult = LoadFlow.run(network, buildLoadFlowParameters());
        assertThat(lfResult.isOk()).isTrue();
        writeIidm(network, name, "solved");
        printSLDs(network, name);
    }

    @Test
    void microGridT4() {
        final var name = "microGridT4";
        var network = readCgmesZip(microGridT4BBAssembled);
        assertThat(network.getSubstationCount()).isEqualTo(3);
        writeIidm(network, name, "init");

        var lfResult = LoadFlow.run(network, buildLoadFlowParameters());
        assertThat(lfResult.isOk()).isTrue();
        writeIidm(network, name, "solved");
        printSLDs(network, name);
    }

    @Test
    void realGrid() {
        final var name = "realGrid";
        var network = readCgmesZip(realGrid);
        assertThat(network.getSubstationCount()).isEqualTo(4791);
        writeIidm(network, name, "init");
        var lfResult = LoadFlow.run(network, buildLoadFlowParameters());
        assertThat(lfResult.isOk()).isTrue();
        writeIidm(network, name, "solved");
    }

    private Network readCgmesZip(final Path zipPath) {
        return readCgmesZip(zipPath, false);
    }

    private Network readCgmesZip(final Path zipPath, final boolean convertBoundary) {
        var props = new Properties();
        props.put("iidm.import.cgmes.convert-boundary", convertBoundary);
        return Network.read(zipPath, LocalComputationManager.getDefault(), ImportConfig.CACHE.get(), props);
    }

    private LoadFlowParameters buildLoadFlowParameters() {
        final var loadFlowParameters = new LoadFlowParameters()
                // slack distribution related
                .setDistributedSlack(true)
                .setBalanceType(LoadFlowParameters.BalanceType.PROPORTIONAL_TO_GENERATION_P_MAX)
                // topology related
                .setConnectedComponentMode(LoadFlowParameters.ConnectedComponentMode.MAIN)
                // regulations & limits related
                .setPhaseShifterRegulationOn(true)
                .setTransformerVoltageControlOn(true)
                .setShuntCompensatorVoltageControlOn(true)
                .setHvdcAcEmulation(true)
                .setUseReactiveLimits(false)
                // resolution method related
                .setVoltageInitMode(LoadFlowParameters.VoltageInitMode.DC_VALUES);
        final var openLoadFlowParameters = new OpenLoadFlowParameters()
                // convergence related
                .setMaxIteration(30)
                // resolution method related
                .setLowImpedanceBranchMode(OpenLoadFlowParameters.LowImpedanceBranchMode.REPLACE_BY_ZERO_IMPEDANCE_LINE)
                .setTransformerVoltageControlMode(OpenLoadFlowParameters.TransformerVoltageControlMode.INCREMENTAL_VOLTAGE_CONTROL)
                .setShuntVoltageControlMode(OpenLoadFlowParameters.ShuntVoltageControlMode.INCREMENTAL_VOLTAGE_CONTROL)
                .setVoltageInitModeOverride(OpenLoadFlowParameters.VoltageInitModeOverride.NONE)
                // slack selection related
                .setSlackBusSelectionMode(SlackBusSelectionMode.MOST_MESHED)
                // features related
                .setVoltageRemoteControl(true)
                .setReactivePowerRemoteControl(true);
        loadFlowParameters.addExtension(OpenLoadFlowParameters.class, openLoadFlowParameters);
        return loadFlowParameters;
    }

    private static void writeIidm(Network network, String folderName, String iidmName) {
        try {
            Files.createDirectories(outDir.resolve(folderName));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        network.write("XIIDM", new Properties(), outDir.resolve(folderName).resolve(iidmName + ".xiidm"));
    }

    private static void printSLDs(final Network network, final String folderName) {
        final Path folder = outDir.resolve(folderName);
        final Path slds = folder.resolve("slds");
        try {
            FileUtils.deleteDirectory(slds.toFile());
            Files.createDirectories(slds);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        network.getVoltageLevelStream().forEach(
                vl -> SingleLineDiagram.draw(network, vl.getId(), slds.resolve(vl.getNameOrId() + "_" + vl.getId() + ".svg"))
        );
        NetworkAreaDiagram nad = new NetworkAreaDiagram(network);
        SvgParameters svgParameters = new SvgParameters().setFixedHeight(1000);
        LayoutParameters layoutParametersNad = new LayoutParameters().setSpringRepulsionFactorForceLayout(0.2);
        nad.draw(folder.resolve("area.svg"), svgParameters, layoutParametersNad);
    }
}
