package sf.net.experimaestro.utils.jpa;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
 *
 * experimaestro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * experimaestro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 */

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.File;


@Converter
public class FileConverter implements AttributeConverter<File, String> {
    @Override
    public String convertToDatabaseColumn(File attribute) {
      return  attribute.getAbsolutePath();
    }

    @Override
    public File convertToEntityAttribute(String dbData) {
        return  new File(dbData);
    }
}
