package vista;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 * Gestor de recursos gráficos para las cartas y reversos del Blackjack.
 * Carga las imágenes desde el classpath (/cartas y /cartas/reversos) garantizando
 * compatibilidad total entre Windows, macOS y ejecutables empaquetados (.jar/.exe).
 * Incluye escalado suave, esquinas redondeadas para el reverso, caché en memoria
 * y un catálogo de reversos preparado para personalización.
 *
 * @author Alejandro Martínez Torres
 * @version 1.0
 * @since 2026-11-19
 */
public class GestorCartas {

    public static final int ANCHO_CARTA = 82;
    public static final int ALTO_CARTA = 118;

    private static final String RUTA_CARTAS = "/cartas/";
    private static final String RUTA_REVERSOS = "/cartas/reversos/";

    // Caché en memoria para no leer del disco en cada turno
    private static final Map<String, ImageIcon> cache = new HashMap<>();

    // Catálogo de reversos disponibles (Nombre visible -> archivo en /cartas/reversos/)
    private static final Map<String, String> reversosDisponibles = new LinkedHashMap<>();
    private static String reversoActual = "Azul Clásico";

    static {
        reversosDisponibles.put("Azul Clásico", "reverso_azul.jpg");
        // Para añadir más reversos en el futuro, añade aquí una línea nueva:
        // reversosDisponibles.put("Rojo Casino", "reverso_rojo.jpg");
    }

    /**
     * Devuelve el ImageIcon correspondiente al texto enviado por el servidor
     * (por ejemplo "As de Picas", "J de Corazones" o "[Oculta]").
     */
    public static ImageIcon obtenerIconoCarta(String textoCarta) {
        if (textoCarta == null || textoCarta.trim().isEmpty()) {
            return crearCartaRespaldo("?");
        }

        String limpia = textoCarta.trim();
        if ("[Oculta]".equalsIgnoreCase(limpia)) {
            return obtenerIconoReverso();
        }

        if (cache.containsKey(limpia)) {
            return cache.get(limpia);
        }

        ImageIcon icono = cargarCartaDesdeRecursos(limpia);
        cache.put(limpia, icono);
        return icono;
    }

    /**
     * Devuelve el ImageIcon del reverso actualmente seleccionado.
     */
    public static ImageIcon obtenerIconoReverso() {
        String claveCache = "REVERSO_" + reversoActual;
        if (cache.containsKey(claveCache)) {
            return cache.get(claveCache);
        }

        String archivo = reversosDisponibles.getOrDefault(reversoActual, "reverso_azul.jpg");
        URL url = GestorCartas.class.getResource(RUTA_REVERSOS + archivo);
        ImageIcon icono;

        if (url != null) {
            icono = procesarImagen(url, true);
        } else {
            icono = crearCartaRespaldo("BACK");
        }

        cache.put(claveCache, icono);
        return icono;
    }

    public static List<String> getReversosDisponibles() {
        return new ArrayList<>(reversosDisponibles.keySet());
    }

    public static String getReversoActual() {
        return reversoActual;
    }

    public static void setReversoActual(String nombreReverso) {
        if (reversosDisponibles.containsKey(nombreReverso)) {
            reversoActual = nombreReverso;
        }
    }

    // --- LÓGICA INTERNA DE MAPEO Y ESCALADO ---

    private static ImageIcon cargarCartaDesdeRecursos(String textoCarta) {
        // Formato esperado de Carta.toString(): "Rango de Palo"
        String[] partes = textoCarta.split(" de ");
        if (partes.length != 2) {
            return crearCartaRespaldo(textoCarta);
        }

        String rangoEn = traducirRango(partes[0].trim());
        String paloEn = traducirPalo(partes[1].trim());
        if (rangoEn == null || paloEn == null) {
            return crearCartaRespaldo(textoCarta);
        }

        // Busca primero la versión con "2" (jack_of_spades2.png) y si no existe, la normal
        URL url = GestorCartas.class.getResource(RUTA_CARTAS + rangoEn + "_of_" + paloEn + "2.png");
        if (url == null) {
            url = GestorCartas.class.getResource(RUTA_CARTAS + rangoEn + "_of_" + paloEn + ".png");
        }

        if (url != null) {
            return procesarImagen(url, false);
        }

        return crearCartaRespaldo(textoCarta);
    }

    private static String traducirRango(String rangoEs) {
        switch (rangoEs.toUpperCase()) {
            case "AS": return "ace";
            case "J":  return "jack";
            case "Q":  return "queen";
            case "K":  return "king";
            case "2": case "3": case "4": case "5":
            case "6": case "7": case "8": case "9": case "10":
                return rangoEs;
            default:
                return null;
        }
    }

    private static String traducirPalo(String paloEs) {
        switch (paloEs.toLowerCase()) {
            case "corazones": return "hearts";
            case "picas":     return "spades";
            case "tréboles":
            case "treboles":  return "clubs";
            case "diamantes": return "diamonds";
            default:
                return null;
        }
    }

    /**
     * Escala la imagen con suavizado de alta calidad y aplica esquinas redondeadas
     * cuando es necesario (por ejemplo en reversos .jpg) para integrarla sobre el tapete.
     */
    private static ImageIcon procesarImagen(URL url, boolean redondearEsquinas) {
        try {
            BufferedImage original = ImageIO.read(url);
            if (original == null) {
                return crearCartaRespaldo("?");
            }

            Image escalada = original.getScaledInstance(ANCHO_CARTA, ALTO_CARTA, Image.SCALE_SMOOTH);
            BufferedImage resultado = new BufferedImage(ANCHO_CARTA, ALTO_CARTA, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = resultado.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            if (redondearEsquinas) {
                RoundRectangle2D clip = new RoundRectangle2D.Float(0, 0, ANCHO_CARTA, ALTO_CARTA, 10, 10);
                g2.setClip(clip);
                g2.drawImage(escalada, 0, 0, null);
                g2.setClip(null);
                g2.setColor(new Color(40, 40, 40));
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, ANCHO_CARTA - 1, ALTO_CARTA - 1, 10, 10));
            } else {
                g2.drawImage(escalada, 0, 0, null);
            }

            g2.dispose();
            return new ImageIcon(resultado);
        } catch (IOException e) {
            return crearCartaRespaldo("?");
        }
    }

    /**
     * Red de seguridad: dibuja una carta básica si faltase algún archivo PNG/JPG.
     */
    private static ImageIcon crearCartaRespaldo(String texto) {
        BufferedImage img = new BufferedImage(ANCHO_CARTA, ALTO_CARTA, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(Color.WHITE);
        g2.fillRoundRect(1, 1, ANCHO_CARTA - 2, ALTO_CARTA - 2, 10, 10);
        g2.setColor(Color.DARK_GRAY);
        g2.drawRoundRect(1, 1, ANCHO_CARTA - 2, ALTO_CARTA - 2, 10, 10);

        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.drawString(texto.length() > 10 ? texto.substring(0, 10) : texto, 8, ALTO_CARTA / 2);
        g2.dispose();
        return new ImageIcon(img);
    }
}