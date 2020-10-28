package ar.edu.itba.client;

import ar.edu.itba.api.Tree;
import ar.edu.itba.api.queryResults.*;
import ar.edu.itba.client.queries.*;
import ar.edu.itba.client.utils.CABACSVParser;
import ar.edu.itba.client.utils.CSVParser;
import ar.edu.itba.api.utils.CommandUtils;
import ar.edu.itba.client.utils.FileUtils;
import ar.edu.itba.client.utils.VancouverCSVParser;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.IList;
import com.hazelcast.core.IMap;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.core.HazelcastInstance;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static ar.edu.itba.api.utils.CommandUtils.JAVA_OPT;

public class Client {
    private static final Logger logger = LoggerFactory.getLogger(Client.class);
    //all the option/parameters/properties
    private static final String QUERY_OPT = "query";
    private static final String CITY_OPT = "city";
    private static final String NODES_ADDRS_OPT = "addresses";
    private static final String IN_PATH_OPT = "inPath";
    private static final String OUT_PATH_OPT = "outPath";
    private static final String MIN_OPT = "min";
    private static final String N_OPT = "n";
    private static final String NAME_OPT = "name";

    private static final String CABA_CITY = "BUE";
    private static final String VANCOUVER_CITY = "VAN";

    private static final String TREES_FILENAME = "arboles";
    private static final String CITIES_FILENAME = "barrios";

    private static final String CSV_EXTENSION = "csv";

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException, ParseException{
        //TODO: Parsear y armar la query pedida
        //query = .....

        //get properties from command line
        Properties props = parseCommandLine(args);

        //get addresses
        String[] clientAddresses = props.getProperty(NODES_ADDRS_OPT).split(";");

        // Config and get hazelcast instance
        logger.info("tp2hazelcast Client Starting ...");
        ClientConfig cfg = new ClientConfig();
        GroupConfig groupConfig = cfg.getGroupConfig();
        groupConfig.setName("tp2-g5");
        groupConfig.setPassword("angi"); //AgusNicoGuidoIgnacio --> ANGI
        ClientNetworkConfig clientNetworkConfig = cfg.getNetworkConfig();
        for (String addr : clientAddresses ) {
            if(addr.charAt(0) == '\''){
                addr = addr.substring(1);
            }
            if(addr.charAt(addr.length() - 1) == '\''){
                addr = addr.substring(0, addr.length() - 1);
            }
            System.out.println(addr);
            clientNetworkConfig.addAddress(addr);
        }

        HazelcastInstance hz = HazelcastClient.newHazelcastClient(cfg);

        String city = props.getProperty(CITY_OPT);
        CSVParser csvParser;
        if (city.equals(CABA_CITY)) {
            csvParser = new CABACSVParser();
        } else if (city.equals(VANCOUVER_CITY)) {
            csvParser = new VancouverCSVParser();
        } else {
            throw new IllegalArgumentException("Supplied city value is unsupported: " + city);
        }
        try {
            csvParser.parseTrees(FileUtils.formatFilePath(props.getProperty(IN_PATH_OPT), TREES_FILENAME + city, CSV_EXTENSION));
        } catch (IOException e) {
            System.err.println(e.getMessage());
            throw e;
        } finally {
            hz.shutdown();
        }

        //load tree list provided above
        IList<Tree> iTreeList = hz.getList("tree-list");
        iTreeList.addAll(csvParser.getTrees());

        //if query1 add the population map
        if(props.getProperty(QUERY_OPT).equals("1")){
            csvParser.parseCities(FileUtils.formatFilePath(props.getProperty(IN_PATH_OPT), CITIES_FILENAME + city, CSV_EXTENSION));

            IMap<String, Long> populationsIMap = hz.getMap("populations-map");
            populationsIMap.putAll(csvParser.getPopulation());
        }

        switch (props.getProperty(QUERY_OPT)){
            case "1":
                Query1 query1 = new Query1(hz);
                List<Query1Result> results1 = query1.getResult();
                // TODO: mandar resultados al out csv
                break;
            case "2":
                Query2 query2 = new Query2(hz, Long.parseLong(props.getProperty(MIN_OPT)));
                List<Query2Result> results2 = query2.getResult();
                // TODO: mandar resultados al out csv
                break;
            case "3":
                Query3 query3 = new Query3(hz, Long.parseLong(props.getProperty(N_OPT)));
                List<Query3Result> results3 = query3.getResult();
                // TODO: mandar resultados al out csv
                break;
            case "4":
                Query4 query4 = new Query4(hz, props.getProperty(NAME_OPT), Long.parseLong(props.getProperty(MIN_OPT)));
                List<Query4Result> results4 = query4.getResult();
                // TODO: mandar resultados al out csv
                break;
            case "5":
                Query5 query5 = new Query5(hz);
                List<Query5Result> results5 = query5.getResult();
                // TODO: mandar resultados al out csv
                break;
        }

        //remove all added objects
        hz.getDistributedObjects().forEach(DistributedObject::destroy);

        //TODO: Escritura de archivos de salida

        // disconnect from cluster
        hz.shutdown();
    }

    //TODO: check this vs System.getProperty("property name");
    private static Properties parseCommandLine(String[] args) throws ParseException {
        // basic options needed
        Option queryOption = new Option(JAVA_OPT, "specifies the query to execute");
        queryOption.setArgName(QUERY_OPT);
        queryOption.setRequired(true);

        Option cityOption = new Option(JAVA_OPT, "specifies the city dataset used");
        cityOption.setArgName(CITY_OPT);
        cityOption.setRequired(true);

        Option addressesOption = new Option(JAVA_OPT, "specifies the ip addresses of the nodes");
        addressesOption.setArgName(NODES_ADDRS_OPT);
        addressesOption.setRequired(true);

        Option inPathOption = new Option(JAVA_OPT, "specifies the path to the input files");
        inPathOption.setArgName(IN_PATH_OPT);
        inPathOption.setRequired(true);

        Option outPathOption = new Option(JAVA_OPT, "specifies the path to the output files");
        outPathOption.setArgName(OUT_PATH_OPT);
        outPathOption.setRequired(true);

        //return the properties given
        Properties properties = CommandUtils.parseCommandLine(
                args,
                queryOption, cityOption,addressesOption,inPathOption,outPathOption
        );

        properties.putAll(parseQueryArgsCommandLine(Integer.parseInt(properties.getProperty(QUERY_OPT)), args));

        return properties;
    }

    // Apache Commons CLI necesita que parseemos los opcionales antes, porque
    // el parser siempre chequea requeridos, y si mezclamos opciones requeridas
    // y no requeridas estas ultimas se tomaran como requeridas
    private static Properties parseQueryArgsCommandLine(int query, String[] args) throws ParseException {
        //specific options

        Collection<Option> options = new LinkedList<>();
        if (query == 2 || query == 4) {
            Option minOpt = new Option(JAVA_OPT, "specifies the path to the output files");
            minOpt.setArgName(MIN_OPT);
            minOpt.setRequired(true);
            options.add(minOpt);
        }
        if (query == 3) {
            Option nOpt = new Option(JAVA_OPT, "specifies the path to the output files");
            nOpt.setArgName(N_OPT);
            nOpt.setRequired(true);
            options.add(nOpt);
        }
        if (query == 4) {
            Option nameOpt = new Option(JAVA_OPT, "specifies the path to the output files");
            nameOpt.setArgName(NAME_OPT);
            nameOpt.setRequired(true);
            options.add(nameOpt);
        }

        return CommandUtils.parseCommandLine(
                args,
                options.toArray(new Option[3])
        );
    }
}
