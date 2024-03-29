package de.pewpewproject.lasertag.dummy;

import de.pewpewproject.lasertag.resource.WebResourceManager;
import de.pewpewproject.lasertag.common.types.Tuple;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class DummyWebResourceManager extends WebResourceManager {

    @Override
    public List<Tuple<Identifier, Resource>> getWebSite(Identifier ignored) {
        var list = new LinkedList<Tuple<Identifier, Resource>>();

        var baseDir = Path.of(System.getProperty("user.dir"),"src", "main", "resources", "assets", "lasertag", "web");

        try (var stream = Files.walk(baseDir)) {

            var paths = stream.filter(Files::isRegularFile).toList();

            for (var path : paths) {
                var idPath = path.toString().replace('\\', '/').split("lasertag/web/")[1];

                list.add(new Tuple<>(new Identifier(idPath), new Resource("lasertag", () -> new FileInputStream(path.toString()))));
            }

            return list;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
