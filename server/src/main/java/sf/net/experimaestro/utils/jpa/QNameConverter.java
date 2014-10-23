package sf.net.experimaestro.utils.jpa;

import sf.net.experimaestro.manager.QName;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class QNameConverter implements AttributeConverter<QName, String> {
    @Override
    public String convertToDatabaseColumn(QName attribute) {
        return attribute.toString();
    }

    @Override
    public QName convertToEntityAttribute(String dbData) {
        return QName.parse(dbData);
    }
}
