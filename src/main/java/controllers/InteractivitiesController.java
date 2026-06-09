package controllers;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RequestScoped
@jakarta.ws.rs.Path("/")
public class InteractivitiesController {

    @Inject Config config;

    private Path interactivitiesDir() {
        String localRoot = config.getString("com.horstmann.codecheck.storage.local");
        return Path.of(localRoot, "Interactivities");
    }

    @GET
    @jakarta.ws.rs.Path("/interactivities/{name}")
    @Produces(MediaType.TEXT_HTML)
    public Response serveInteractivity(@PathParam("name") String name) {
        try {
            // Sanitize: only allow alphanumeric, hyphens, underscores, dots
            if (!name.matches("[a-zA-Z0-9._-]+")) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Invalid name").build();
            }
            // Add .xhtml extension if not present
            String fileName = name.endsWith(".xhtml") ? name : name + ".xhtml";
            Path file = interactivitiesDir().resolve(fileName);
            if (!Files.isRegularFile(file)) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Interactivity not found: " + name).build();
            }
            String content = Files.readString(file);
            // Rewrite relative asset paths to absolute paths served by our server
            content = content.replace("src=\"script/horstmann_all_min.js\"",
                    "src=\"/interactivities/script/horstmann_all_min.js\"");
            content = content.replace("href=\"css/horstmann_all_min.css\"",
                    "href=\"/interactivities/css/horstmann_all_min.css\"");
            content = content.replace("src=\"assets/receiveMessage.js\"",
                    "src=\"/assets/receiveMessage.js\"");
            return Response.ok(content).type("text/html;charset=UTF-8").build();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build();
        }
    }

    @GET
    @jakarta.ws.rs.Path("/interactivities/script/{file}")
    public Response serveScript(@PathParam("file") String file) {
        return serveAsset("script/" + file, "application/javascript");
    }

    @GET
    @jakarta.ws.rs.Path("/interactivities/css/{file}")
    public Response serveCss(@PathParam("file") String file) {
        return serveAsset("css/" + file, "text/css");
    }

    private Response serveAsset(String relativePath, String mediaType) {
        try {
            if (!relativePath.matches("[a-zA-Z0-9._/-]+")) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Invalid path").build();
            }
            Path file = interactivitiesDir().resolve(relativePath);
            if (!Files.isRegularFile(file)) {
                return Response.status(Response.Status.NOT_FOUND).entity("Not found").build();
            }
            byte[] content = Files.readAllBytes(file);
            return Response.ok(content).type(mediaType).build();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build();
        }
    }
}
