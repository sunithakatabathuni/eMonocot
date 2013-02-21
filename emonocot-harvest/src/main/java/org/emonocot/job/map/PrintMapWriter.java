package org.emonocot.job.map;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.emonocot.model.BaseData;
import org.emonocot.model.Place;
import org.emonocot.model.Taxon;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.utils.PJsonObject;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.batch.item.ItemWriter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;

public class PrintMapWriter implements ItemWriter<BaseData> {
	
	private MapPrinter mapPrinter;
	
	private Resource config;
	
	private FileSystemResource outputDirectory;
    
    private ObjectMapper objectMapper;
    
	public void setMapPrinter(MapPrinter mapPrinter) {
		this.mapPrinter = mapPrinter;
	}

	public void setConfig(Resource config) {
		this.config = config;
	}

	public void setOutputDirectory(FileSystemResource outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	private String referer = null;

	@Override
	public void write(List<? extends BaseData> items) throws Exception {
		for(BaseData item : items) {
			if(item instanceof Place) {
				Place place = (Place)item;
				MapSpec mapSpec = new MapSpec();
				mapSpec.setDpi(300);
				
				TMSLayer tmsLayer = new TMSLayer();
				tmsLayer.setLayer("eMonocot");
				tmsLayer.setBaseURL("http://e-monocot.org/tiles/");
				tmsLayer.setMaxExtent(new double[] {-20037508.3392,-20037508.3392,20037508.3392,20037508.3392});
				tmsLayer.setResolutions(new double[] {156543.0339,78271.51695,39135.758475,19567.8792375,9783.93961875,4891.969809375,2445.9849046875});
				tmsLayer.setFormat("png");
				mapSpec.getLayers().add(tmsLayer);
				WMSLayer wmsLayer = new WMSLayer();
				wmsLayer.setLayers(new String[] {"emonocot:place"});
				wmsLayer.setBaseURL("http://e-monocot.org/geoserver/wms");
				wmsLayer.setMaxExtent(new double[] {-20037508.3392,-20037508.3392,20037508.3392,20037508.3392});
				wmsLayer.setResolutions(new double[] {156543.0339,78271.51695,39135.758475,19567.8792375,9783.93961875,4891.969809375,2445.9849046875});
				wmsLayer.setFormat("image/png");
				wmsLayer.setStyles(new String[] {"eMonocot"});
				wmsLayer.getCustomParams().put("featureid",place.getMapFeatureId().toString());
				mapSpec.getLayers().add(wmsLayer);
				Page page = new Page();

				CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:4326");
				CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:3857");

				MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
				GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
			    
			    
			    Point bottomLeft = (Point)JTS.transform( geometryFactory.createPoint(new Coordinate(place.getEnvelope().getMinY(),place.getEnvelope().getMinX())), transform);
				Point topRight = (Point)JTS.transform( geometryFactory.createPoint(new Coordinate(place.getEnvelope().getMaxY(),place.getEnvelope().getMaxX())), transform);
				
				page.setBbox(new double[] {bottomLeft.getCoordinate().x,bottomLeft.getCoordinate().y, topRight.getCoordinate().x,topRight.getCoordinate().y});
				
				mapSpec.getPages().add(page);
				
				String json = objectMapper.writeValueAsString(mapSpec);
				mapPrinter.setYamlConfigFile(config.getFile());
				System.out.println(json);
				PJsonObject jsonSpec = MapPrinter.parseSpec(json);
				File outputFile = new File(outputDirectory.getFile(),place.getId() + ".png");
				mapPrinter.print(jsonSpec, new FileOutputStream(outputFile), referer);
				
			} else if(item instanceof Taxon) {
				
			}
		}
		
	}

}