package applet.config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.swixml.Localizer;

import sidplay.ini.intf.IConfig;
import applet.config.annotations.ConfigTransient;

public class ConfigModel extends DefaultTreeModel {

	private Localizer localizer;

	public ConfigModel() {
		super(null);
	}

	@Override
	public boolean isLeaf(Object node) {
		DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
		if (treeNode.getUserObject() instanceof Method) {
			return false;
		} else if (treeNode.getUserObject() instanceof Field) {
			return true;
		} else if (isSimpleField(treeNode.getUserObject().getClass())) {
			return true;
		}
		return false;
	}

	@Override
	public int getChildCount(Object parent) {
		ConfigNode treeNode = (ConfigNode) parent;
		try {
			Object obj;
			if (treeNode.getUserObject() instanceof Method) {
				Method method = (Method) treeNode.getUserObject();
				Object methodObject = treeNode.getMethodObject();
				obj = method.invoke(methodObject);
				if (obj instanceof List) {
					List<?> list = (List<?>) obj;
					return list.size();
				}
			} else {
				obj = treeNode.getUserObject();
			}
			Field[] declaredFields = obj.getClass().getDeclaredFields();
			int fieldCount = 0;
			for (Field field : declaredFields) {
				if (isIgnorableField(field)) {
					continue;
				}
				fieldCount++;
			}
			return fieldCount;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public Object getChild(Object parent, int index) {
		ConfigNode treeNode = (ConfigNode) parent;
		try {
			Object obj;
			if (treeNode.getUserObject() instanceof Method) {
				Method method = (Method) treeNode.getUserObject();
				Object methodObject = treeNode.getMethodObject();
				obj = method.invoke(methodObject);
				if (obj instanceof List) {
					List<?> list = (List<?>) obj;
					return new ConfigNode(obj, list.get(index), localizer);
				}
			} else {
				obj = treeNode.getUserObject();
			}
			Field[] fields = obj.getClass().getDeclaredFields();
			Object[] childs = new Object[fields.length];
			int fieldCount = 0;
			for (Field field : fields) {
				if (isIgnorableField(field)) {
					continue;
				}
				if (isSimpleField(field.getType())) {
					childs[fieldCount++] = field;
				} else {
					Method method = obj.getClass().getMethod(
							ConfigNode.getGetterMethod(field, true));
					childs[fieldCount++] = method;
				}
			}
			return new ConfigNode(obj, childs[index], localizer);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		return super.getIndexOfChild(parent, child);
	}

	public void setRootUserObject(Localizer localizer, IConfig config) {
		this.localizer = localizer;
		setRoot(new ConfigNode(null, config, localizer));
	}

	private boolean isSimpleField(Class<?> cls) {
		return cls.isEnum() || cls.isPrimitive()
				|| cls.getPackage().getName().startsWith("java.lang");
	}

	private boolean isIgnorableField(Field field) {
		return field.getAnnotation(ConfigTransient.class) != null;
	}

}
