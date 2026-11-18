package com.craftcore.service;

import com.craftcore.app.AppContext;
import com.craftcore.dao.orm.PlayerDAO;
import com.craftcore.dao.orm.SanctionDAO;
import com.craftcore.model.entity.Admin;
import com.craftcore.model.entity.Player;
import com.craftcore.model.entity.Sanction;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;

import java.io.File;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio para compilación y exportación de reportes JasperReports en PDF y Excel (.xlsx) (RA4).
 */
public class ReportGeneratorService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public enum ReportType {
        SANCTIONS("Reporte de Sanciones"),
        PLAYER_ACTIVITY("Actividad de Jugadores");

        private final String displayName;

        ReportType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum ExportFormat {
        PDF,
        EXCEL
    }

    private final SanctionDAO sanctionDAO = new SanctionDAO();
    private final PlayerDAO playerDAO = new PlayerDAO();

    /**
     * Compila la plantilla JRXML correspondiente, carga datos desde PostgreSQL y exporta a PDF o Excel.
     */
    public void generateReport(ReportType type, ExportFormat format, LocalDate desde, LocalDate hasta, File outputFile)
            throws Exception {

        String jrxmlPath = (type == ReportType.SANCTIONS)
                ? "/com/craftcore/reports/reporte_sanciones.jrxml"
                : "/com/craftcore/reports/reporte_actividad_jugadores.jrxml";

        List<?> rows = (type == ReportType.SANCTIONS)
                ? buildSanctionRows(desde, hasta)
                : buildPlayerActivityRows();

        Map<String, Object> params = buildCommonParams(type, desde, hasta, rows.size());

        try (InputStream templateStream = getClass().getResourceAsStream(jrxmlPath)) {
            if (templateStream == null) {
                throw new IllegalStateException("No se encontró la plantilla JRXML: " + jrxmlPath);
            }
            JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(rows);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, dataSource);

            if (format == ExportFormat.PDF) {
                JasperExportManager.exportReportToPdfFile(jasperPrint, outputFile.getAbsolutePath());
            } else {
                exportToExcel(jasperPrint, outputFile);
            }
        }
    }

    private void exportToExcel(JasperPrint jasperPrint, File outputFile) throws JRException {
        JRXlsxExporter exporter = new JRXlsxExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputFile));

        SimpleXlsxReportConfiguration config = new SimpleXlsxReportConfiguration();
        config.setOnePagePerSheet(false);
        config.setDetectCellType(true);
        config.setRemoveEmptySpaceBetweenRows(true);
        exporter.setConfiguration(config);
        exporter.exportReport();
    }

    private Map<String, Object> buildCommonParams(ReportType type, LocalDate desde, LocalDate hasta, int total) {
        Map<String, Object> params = new HashMap<>();
        params.put("P_TITULO", type.toString());

        String rango = (desde == null && hasta == null)
                ? "Rango: Histórico completo"
                : "Rango: " + (desde != null ? desde : "Inicio") + " a " + (hasta != null ? hasta : "Hoy");
        params.put("P_RANGO_FECHAS", rango);

        Admin admin = AppContext.getInstance().getCurrentAdmin();
        params.put("P_GENERADO_POR", admin != null ? admin.getUsername() : "Sistema");
        params.put("P_TOTAL_REGISTROS", total);
        return params;
    }

    private List<SanctionReportRow> buildSanctionRows(LocalDate desde, LocalDate hasta) {
        return sanctionDAO.findByFilters(desde, hasta, "TODOS").stream()
                .map(s -> new SanctionReportRow(
                        s.getIdSancion(),
                        s.getJugador() != null ? s.getJugador().getNickname() : "—",
                        s.getTipo(),
                        s.getMotivo(),
                        s.getEstado(),
                        s.getAdmin() != null ? s.getAdmin().getUsername() : "—",
                        s.getFechaInicio() != null ? s.getFechaInicio().format(DATE_FMT) : "—"
                ))
                .toList();
    }

    private List<PlayerActivityRow> buildPlayerActivityRows() {
        List<Sanction> allSanctions = sanctionDAO.findAllWithDetails();
        return playerDAO.findAll().stream()
                .map(p -> {
                    int sanctionCount = (int) allSanctions.stream()
                            .filter(s -> s.getJugador() != null && p.getIdJugador().equals(s.getJugador().getIdJugador()))
                            .count();
                    return new PlayerActivityRow(
                            p.getIdJugador(),
                            p.getNickname(),
                            p.getUuid(),
                            Boolean.TRUE.equals(p.getEsOp()) ? "Sí" : "No",
                            Boolean.TRUE.equals(p.getEnWhitelist()) ? "Sí" : "No",
                            p.getPrimeraConexion() != null ? p.getPrimeraConexion().format(DATE_FMT) : "—",
                            sanctionCount
                    );
                })
                .toList();
    }

    public static class SanctionReportRow {
        private final Integer idSancion;
        private final String jugador;
        private final String tipo;
        private final String motivo;
        private final String estado;
        private final String operador;
        private final String fecha;

        public SanctionReportRow(Integer idSancion, String jugador, String tipo, String motivo,
                                 String estado, String operador, String fecha) {
            this.idSancion = idSancion;
            this.jugador = jugador;
            this.tipo = tipo;
            this.motivo = motivo;
            this.estado = estado;
            this.operador = operador;
            this.fecha = fecha;
        }

        public Integer getIdSancion() { return idSancion; }
        public String getJugador() { return jugador; }
        public String getTipo() { return tipo; }
        public String getMotivo() { return motivo; }
        public String getEstado() { return estado; }
        public String getOperador() { return operador; }
        public String getFecha() { return fecha; }
    }

    public static class PlayerActivityRow {
        private final Integer idJugador;
        private final String nickname;
        private final String uuid;
        private final String esOp;
        private final String enWhitelist;
        private final String primeraConexion;
        private final Integer totalSanciones;

        public PlayerActivityRow(Integer idJugador, String nickname, String uuid, String esOp,
                                 String enWhitelist, String primeraConexion, Integer totalSanciones) {
            this.idJugador = idJugador;
            this.nickname = nickname;
            this.uuid = uuid;
            this.esOp = esOp;
            this.enWhitelist = enWhitelist;
            this.primeraConexion = primeraConexion;
            this.totalSanciones = totalSanciones;
        }

        public Integer getIdJugador() { return idJugador; }
        public String getNickname() { return nickname; }
        public String getUuid() { return uuid; }
        public String getEsOp() { return esOp; }
        public String getEnWhitelist() { return enWhitelist; }
        public String getPrimeraConexion() { return primeraConexion; }
        public Integer getTotalSanciones() { return totalSanciones; }
    }
}
