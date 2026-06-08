package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.horstmann.codecheck.checker.Util;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import services.CodeCheck;
import services.ServiceException;

import javax.script.ScriptException;
import java.io.IOException;

@RequestScoped
@jakarta.ws.rs.Path("/")
public class FilesController {
    @Inject services.Files filesService;
    @Context UriInfo uriInfo;
    @Context HttpHeaders headers;
    @CookieParam("ccid") String ccid;

    @GET
    @jakarta.ws.rs.Path("/files/{repo}/{problem}")
    @Produces(MediaType.TEXT_HTML)
    public Response filesHTML2(@PathParam("repo") String repo, @PathParam("problem") String problemName)  {
        try {
            if (ccid == null) ccid = Util.createPronouncableUID();
            String result = filesService.filesHTML2(HttpUtil.prefix(uriInfo, headers), repo, problemName, ccid);
            return Response.ok(result).cookie(HttpUtil.buildCookie("ccid", ccid, HttpUtil.isSecure(uriInfo, headers))).build();
        } catch (ServiceException | IOException | ScriptException | NoSuchMethodException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @jakarta.ws.rs.Path("/files/{problem}")
    @Produces(MediaType.TEXT_HTML)
    public Response filesHTML2(@PathParam("problem") String problemName) throws IOException, ScriptException, NoSuchMethodException {
        return filesHTML2(CodeCheck.DEFAULT_REPO, problemName);
    }

    @GET
    @jakarta.ws.rs.Path("/files")
    @Produces(MediaType.TEXT_HTML)
    public Response filesHTML2queryParams(@QueryParam("repo") String repo, @QueryParam("problem") String problemName) throws IOException, ScriptException, NoSuchMethodException {
        if (repo == null) repo = CodeCheck.DEFAULT_REPO;
        return filesHTML2(repo, problemName);
    }

    @GET
    @jakarta.ws.rs.Path("/tracer/{repo}/{problem}")
    @Produces(MediaType.TEXT_HTML)
    public Response tracer(@PathParam("repo") String repo, @PathParam("problem") String problemName) {
        try {
            if (ccid == null) ccid = Util.createPronouncableUID();
            String result = filesService.tracer(repo, problemName, ccid);
            return Response.ok(result).cookie(HttpUtil.buildCookie("ccid", ccid, HttpUtil.isSecure(uriInfo, headers))).build();
        } catch (ServiceException | ScriptException | NoSuchMethodException | IOException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @jakarta.ws.rs.Path("/tracer/{problem}")
    @Produces(MediaType.TEXT_HTML)
    public Response tracer(@PathParam("problem") String problemName) throws IOException {
        return tracer(CodeCheck.DEFAULT_REPO, problemName);
    }

    @GET
    @jakarta.ws.rs.Path("/fileData/{repo}/{problem}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response fileData(@PathParam("repo") String repo, @PathParam("problem") String problemName)  {
        try {
            if (ccid == null) ccid = Util.createPronouncableUID();
            ObjectNode result = filesService.fileData(repo, problemName, ccid);
            return Response.ok(result).cookie(HttpUtil.buildCookie("ccid", ccid, HttpUtil.isSecure(uriInfo, headers))).build();
        } catch (ServiceException | IOException | ScriptException | NoSuchMethodException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
    @GET
    @jakarta.ws.rs.Path("/fileData/{problem}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response fileData(@PathParam("problem") String problemName) throws IOException {
        return fileData(CodeCheck.DEFAULT_REPO, problemName);
    }

    /* Legacy
    @GET
    @jakarta.ws.rs.Path("/fileData")
    @Produces(MediaType.APPLICATION_JSON)
    public Response fileDataQueryParams(@QueryParam("repo") String repo, @QueryParam("problem") String problemName) throws IOException {
        if (repo == null) repo = CodeCheck.DEFAULT_REPO;
        return fileData(repo, problemName);
    }
     */
}