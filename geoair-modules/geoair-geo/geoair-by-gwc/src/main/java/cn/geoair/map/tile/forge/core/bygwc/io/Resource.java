/**
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * <p>You should have received a copy of the GNU Lesser General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * <p>Copyright 2019
 */
package cn.geoair.map.tile.forge.core.bygwc.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Resource {

    /** The size of the resource in bytes. */
    long getSize();

    /** An InputStream backed by the resource. */
    InputStream getInputStream() throws IOException;

    /** An OutputStream backed by the resource. Writes are appended to the resource. */
    OutputStream getOutputStream() throws IOException;

    byte[] getByteData() throws IOException;

    /**
     * The time the resource was last modified.
     *
     * @see System#currentTimeMillis
     */
    long getLastModified();
}
