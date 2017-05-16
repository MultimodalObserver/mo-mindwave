package mo.eeg.visualization.attention;


import bibliothek.util.xml.XElement;
import bibliothek.util.xml.XIO;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import mo.core.plugin.Extends;
import mo.core.plugin.Extension;
import mo.organization.Configuration;
import mo.organization.ProjectOrganization;
import mo.organization.StagePlugin;
import mo.visualization.VisualizationProvider;

@Extension(
        xtends = {
            @Extends(
                    extensionPointId = "mo.visualization.VisualizationProvider"
            )
        }
)
public class EEGAttPlugin implements VisualizationProvider {

    private static final String NAME = "EEG Att Visualization";
    
    private List<Configuration> configurations;
    
    private final static Logger logger = Logger.getLogger(EEGAttPlugin.class.getName());

    public EEGAttPlugin() {
        configurations = new ArrayList<>();
    }
    
    
    
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Configuration initNewConfiguration(ProjectOrganization organization) {
        AttConfigDialog d = new AttConfigDialog(organization);
        
        if (d.showDialog()) {
            AttConfig c = new AttConfig();
            c.setId(d.getConfigurationName());
            configurations.add(c);
            return c;
        }
        
        return null;
    }

    @Override
    public List<Configuration> getConfigurations() {
        return configurations;
    }

    @Override
    public StagePlugin fromFile(File file) {
        if (file.isFile()) {
            try {
                EEGAttPlugin mc = new EEGAttPlugin();
                XElement root = XIO.readUTF(new FileInputStream(file));
                XElement[] pathsX = root.getElements("path");
                for (XElement pathX : pathsX) {
                    String path = pathX.getString();
                    AttConfig c = new AttConfig();
                    Configuration config = c.fromFile(new File(file.getParentFile(), path));
                    if (config != null) {
                        mc.configurations.add(config);
                    }
                }
                return mc;
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    @Override
    public File toFile(File parent) {
        File file = new File(parent, "attention-visualization.xml");
        if (!file.isFile()) {
            try {
                file.createNewFile();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        XElement root = new XElement("vis");
        for (Configuration config : configurations) {
            File p = new File(parent, "attention-visualization");
            p.mkdirs();
            File f = config.toFile(p);

            XElement path = new XElement("path");
            Path parentPath = parent.toPath();
            Path configPath = f.toPath();
            path.setString(parentPath.relativize(configPath).toString());
            root.addElement(path);
        }
        try {
            XIO.writeUTF(root, new FileOutputStream(file));
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        return file;
    }
    
}
