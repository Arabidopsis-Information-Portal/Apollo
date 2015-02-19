package org.bbop.apollo.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bbop.apollo.web.config.ServerConfiguration;
import org.bbop.apollo.web.data.DataAdapterGroupView;
import org.bbop.apollo.web.user.Permission;
import org.bbop.apollo.web.user.UserManager;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 */
@WebServlet(name = "/sequences", urlPatterns = {"/sequences"}, asyncSupported = true)
public class SelectTrackServlet extends HttpServlet {

    final Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);
    private ServerConfiguration serverConfig;
//    private String databaseDir;
//    private Set<String> allStatusList = new TreeSet<String>();
//    private BioObjectConfiguration bioObjectConfiguration;

    private final Integer DEFAULT_LIST_SIZE = 10;


    @Override
    public void init() throws ServletException {
        try {
            serverConfig = new ServerConfiguration(getServletContext());
        } catch (Exception e) {
            throw new ServletException(e);
        }
//        InputStream gbolMappingStream = getServletContext().getResourceAsStream(serverConfig.getGBOLMappingFile());

//        for (ServerConfiguration.AnnotationInfoEditorConfiguration annotationInfoEditorConfiguration : serverConfig.getAnnotationInfoEditor().values()) {
//            allStatusList.addAll(annotationInfoEditorConfiguration.getStatus());
//        }

//        bioObjectConfiguration = new BioObjectConfiguration(gbolMappingStream);
        if (!UserManager.getInstance().isInitialized()) {
            ServerConfiguration.UserDatabaseConfiguration userDatabase = serverConfig.getUserDatabase();
            try {
                UserManager.getInstance().initialize(userDatabase.getDriver(), userDatabase.getURL(), userDatabase.getUserName(), userDatabase.getPassword());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
//        databaseDir = serverConfig.getDataStoreDirectory();
    }

    /**
     * Generate a record for a feature that includes the name, type, link to browser, and last modified date.
     */


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = (String) request.getSession(true).getAttribute("username");
        Map<String, Integer> permissions;
        try {
            permissions = UserManager.getInstance().getPermissionsForUser(username);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
//        BufferedReader in = new BufferedReader(new InputStreamReader(request.getServletContext().getResourceAsStream(serverConfig.getTrackNameComparator())));
//        String line;
//        String lineString = "";
//        while ((line = in.readLine()) != null) {
//            lineString += line + "\n";
////            out.println(line);
//        }

        Object maximumString = request.getParameter("maximum");
        Object minLengthString = request.getParameter("minLength");
        Object maxLengthString = request.getParameter("maxLength");

        int offset = 0;
        Object offsetString = request.getParameter("offset");
        if (offsetString != null && offsetString.toString().length() > 0) {
            try {
                offset = Integer.parseInt(offsetString.toString());
            } catch (NumberFormatException e) {
                logger.error("Problem parsing offset string: "+offsetString);
                offset = 0 ;
            }
        }

        Integer minLength = 0 ;
        try {
            minLength = (minLengthString != null && minLengthString.toString().trim().length() > 0) ? Integer.parseInt(minLengthString.toString()) : null;
        } catch (NumberFormatException e) {
            log("Failed to parse min string",e);
        }
        Integer maxLength = Integer.MAX_VALUE ;
        try {
            maxLength = (maxLengthString != null && maxLengthString.toString().trim().length() > 0) ? Integer.parseInt(maxLengthString.toString()) : null;
        } catch (NumberFormatException e) {
            log("Failed to parse max string",e);
        }

//        Object organism = request.getParameter("organism");
        Object name = request.getParameter("name");

        int maximum = DEFAULT_LIST_SIZE;
        if (maximumString != null) {
//            if(maximumString.equals("All")){
//                maximum = Integer.MAX_VALUE;
//            }
            maximum = Integer.parseInt(maximumString.toString());
        }

        int count = 0;
        Collection<ServerConfiguration.TrackConfiguration> tracks = serverConfig.getTracks().values();

//        System.out.println("# of tracks: " + tracks.size());
        boolean isAdmin = false;
        List<List<String>> trackTableList = new ArrayList<>();
        List<ServerConfiguration.TrackConfiguration> trackList = new ArrayList<>();


        String organism = null;

        Iterator<ServerConfiguration.TrackConfiguration> iterator = tracks.iterator();
        if (username != null) {
            while (iterator.hasNext() && count < maximum + offset) {
                ServerConfiguration.TrackConfiguration track = iterator.next();
                trackList.add(track);
                Integer permission = permissions.get(track.getName());
                organism = track.getOrganism();
//                System.out.println("count [" + count + "] / maximum [" + maximum + "] offset[" + offset + "]");
                if (permission == null) {
                    permission = 0;
                }
                if ((permission & Permission.USER_MANAGER) == Permission.USER_MANAGER) {
                    isAdmin = true;
                }
                boolean matches = true;
                if ((permission & Permission.READ) == Permission.READ) {

                    if (name != null && name.toString().trim().length() > 0) {
                        matches = matches && track.getName().toUpperCase().contains(name.toString().toUpperCase());
                    }
                    if (minLength != null) {
                        matches = matches && track.getSourceFeature().getSequenceLength() >= minLength;
                    }
                    if (maxLength != null) {
                        matches = matches && track.getSourceFeature().getSequenceLength() <= maxLength;
                    }

                    if (matches) {
                        if (count < offset) {
                            ++count;
                        } else {

                            List<String> trackRow = new ArrayList<>();
                            trackRow.add(String.format("<input type=\"checkbox\" class=\"track_select\" id=\"%s\"/>", track.getName()));
//                            trackRow.add(track.getOrganism());
                            trackRow.add(String.format("<a target=\"_blank\" href=\"jbrowse/?loc=%s\">%s</a>", track.getSourceFeature().getUniqueName(), track.getSourceFeature().getUniqueName()));
                            trackRow.add(String.format("%d", track.getSourceFeature().getSequenceLength()));
                            trackTableList.add(trackRow);
                            ++count;
                        }
                    }
                }
            }
//            System.out.println("has next " + iterator.hasNext());
//            System.out.println("count < maximum + offset " + count + " < " + maximum + " " + offset);
        }

        int permission = !permissions.isEmpty() ? permissions.values().iterator().next() : 0;
        List<DataAdapterGroupView> dataAdapterConfigurationList = new ArrayList<>();
        for (ServerConfiguration.DataAdapterGroupConfiguration groupConf : serverConfig.getDataAdapters().values()) {
            if ((permission & Permission.getValueForPermission(groupConf.getPermission())) >= 1) {
                DataAdapterGroupView dataAdapterGroupView = new DataAdapterGroupView(groupConf);
                dataAdapterConfigurationList.add(dataAdapterGroupView);
            }
//            }
        }


        request.setAttribute("isAdmin", isAdmin);
        request.setAttribute("username", username);
        request.setAttribute("dataAdapters", dataAdapterConfigurationList);
        request.setAttribute("tracks", trackList);

        List<String> allTrackIds = new ArrayList<>();
        for (ServerConfiguration.TrackConfiguration thisTrack : tracks) {
            Integer thisPermission = permissions.get(thisTrack.getName());
//                System.out.println("count ["+count+"] / maximum ["+maximum +"]");
            if (thisPermission == null) {
                thisPermission = 0;
            }
            if ((thisPermission & Permission.USER_MANAGER) == Permission.USER_MANAGER) {
                isAdmin = true;
            }
            if ((thisPermission & Permission.READ) == Permission.READ || isAdmin) {
                allTrackIds.add(thisTrack.getName());
            }
        }

        request.setAttribute("hasPrevious", offset > 0);
        request.setAttribute("hasNext", iterator.hasNext());

        request.setAttribute("allTrackIds", allTrackIds);
        request.setAttribute("trackViews", trackTableList);
        request.setAttribute("trackCount", tracks.size());

        // filter attributes
        request.setAttribute("maximum", maximum);
        request.setAttribute("minLength", minLength);
        request.setAttribute("maxLength", maxLength);
        request.setAttribute("organism", organism);
        request.setAttribute("name", name);
        request.setAttribute("offset", offset);

        RequestDispatcher view = request.getRequestDispatcher("/tracks.jsp");
        view.forward(request, response);
    }

//    @Override
//    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        super.doGet(req, resp);
//    }
}
