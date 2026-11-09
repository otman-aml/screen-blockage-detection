package com.ocp.screenblockage.entity.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

/**
 * Convertit un polygone de masque {@code List<List<Double>>} en JSON (TEXT)
 * pour la base, et inverse.
 *
 * <p>Regle : le backend conserve le masque sous forme de liste de coordonnees
 * [x, y] (tel que recu du ai-service) ; aucune matrice binaire n'est stockee.
 */
@Converter
public class MaskPolygonConverter implements AttributeConverter<List<List<Double>>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<List<Double>> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de serialiser le masque en JSON", e);
        }
    }

    @Override
    public List<List<Double>> convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return MAPPER.readValue(dbData, new TypeReference<List<List<Double>>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de deserialiser le masque JSON", e);
        }
    }
}