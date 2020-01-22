package gov.taxes.apiautomation.test;


import static org.asciidoctor.Asciidoctor.Factory.create;
import static org.asciidoctor.AttributesBuilder.attributes;
import static org.asciidoctor.OptionsBuilder.options;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.Placement;
import org.asciidoctor.SafeMode;
import org.assertj.core.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import io.github.swagger2markup.GroupBy;
import io.github.swagger2markup.Swagger2MarkupConfig;
import io.github.swagger2markup.Swagger2MarkupConverter;
import io.github.swagger2markup.builder.Swagger2MarkupConfigBuilder;
import io.github.swagger2markup.markup.builder.MarkupLanguage;

/**
 * This class converts swagger supported APIs to html5 with swagger2markup and asciidoctor jars
 * It runs under profile Test so when you build the project it will run the code 
 * (so there is no need to run the project and call the swaggerToHTML function)
 * 
 * In order to work this function out:
 * 
 * 1. 3 properties should be defined in application.properties file:
 * 	service.api.host - this represent the endpoint name or ip
 * 	service.api.ports - this represent the list of ports that hold all the services in the above host
 * 	exportswagger.dir - this represent the directory where the files will be exported
 * 
 * 2. all the services that are running on the above ports have to be up and running
 * 
 * overview
 * For each port defined, the function converts the swagger to adoc file using swagger2markup, 
 * Then, using asciidoctor the function takes the created adoc file and converts it to html5
 * Finally, the adoc file is deleted
 * 
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(initializers = ConfigFileApplicationContextInitializer.class)
@ActiveProfiles("test")
public class ExportSwaggerTest {

	@Autowired
	private Environment env;
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Test
	public  void swaggerToHTML() {

		// get the host that holds all apis and ports of all services 
		String host = env.getProperty("service.api.host");
		List<Object> ports = Arrays.asList(env.getProperty("service.api.ports").split(";"));
		String envDir = env.getProperty("exportswagger.dir");

		//set the file extension to adoc
		Swagger2MarkupConfig config = new Swagger2MarkupConfigBuilder()
				.withMarkupLanguage(MarkupLanguage.ASCIIDOC)
				.withPathsGroupedBy(GroupBy.TAGS)
				.build();

		ports.forEach(port -> {
			try {
				convertSwaggerToHtml(host, envDir, config, port);
			} catch (Exception e ) {
				logger.info("Converting swagger to html on Port " + port + " failed/r/n" + e.toString());
			}
		});

	}

	/**
	 * Converting swagger to html on specific port.
	 * @param host
	 * @param envDir
	 * @param config
	 * @param port
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private void convertSwaggerToHtml(String host, String envDir, Swagger2MarkupConfig config, Object port)
			throws MalformedURLException, IOException {
		Swagger2MarkupConverter converter = Swagger2MarkupConverter
				.from(new URL("http://" + host + ":" + port + "/v2/api-docs"))
				.withConfig(config)
				.build();
		Path outputDirectory =  Paths.get(envDir, port.toString());
		//for each port, takes the API and converts them into a file
		converter.toFile(outputDirectory);

		//convert the adoc file to html5
		Asciidoctor asciidoctor = create();
		Attributes attributes = attributes().backend("html5").get(); 
		attributes.setTableOfContents(Placement.LEFT);
		//table of content depth level
		attributes.setAttribute("toclevels", "3");

		Options options = options().inPlace(true).attributes(attributes).get(); 
		//has to be unsafe mode in order to enable embedded css (asciidoctor.css)
		options.setSafe(SafeMode.UNSAFE);

		asciidoctor.convertFile(
				//takes the created file path from above and convert to html5
				new File(outputDirectory.toString() + ".adoc"),
				options);
		//deleting adoc files after the final html file is created
		Files.deleteIfExists(Paths.get(outputDirectory.toString() + ".adoc"));
	}


}
