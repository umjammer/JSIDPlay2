package server.restful.common.parameter.requestparam;

import com.beust.jcommander.Parameter;

import server.restful.common.Order;

/**
 * LOG requests with filter options.
 * 
 * @author ken
 *
 */
public class LogRequestParamServlet {

	private Long instant = 0L;

	public Long getInstant() {
		return instant;
	}

	@Parameter(names = { "--instant" }, descriptionKey = "INSTANT", order = Integer.MIN_VALUE)
	public void setInstant(Long instant) {
		this.instant = instant;
	}

	private String sourceClassName = "";

	public String getSourceClassName() {
		return sourceClassName;
	}

	@Parameter(names = { "--sourceClassName" }, descriptionKey = "SOURCE_CLASS_NAME", order = Integer.MIN_VALUE + 1)
	public void setSourceClassName(String sourceClassName) {
		this.sourceClassName = sourceClassName;
	}

	private String sourceMethodName = "";

	public String getSourceMethodName() {
		return sourceMethodName;
	}

	@Parameter(names = { "--sourceMethodName" }, descriptionKey = "SOURCE_METHOD_NAME", order = Integer.MIN_VALUE + 2)
	public void setSourceMethodName(String sourceMethodName) {
		this.sourceMethodName = sourceMethodName;
	}

	private String level = "";

	public String getLevel() {
		return level;
	}

	@Parameter(names = { "--level" }, descriptionKey = "LEVEL", order = Integer.MIN_VALUE + 3)
	public void setLevel(String level) {
		this.level = level;
	}

	private String message = "";

	public String getMessage() {
		return message;
	}

	@Parameter(names = { "--message" }, descriptionKey = "MESSAGE", order = Integer.MIN_VALUE + 4)
	public void setMessage(String message) {
		this.message = message;
	}

	private Order order = Order.DESC;

	public Order getOrder() {
		return order;
	}

	@Parameter(names = { "--order" }, descriptionKey = "ORDER", order = Integer.MIN_VALUE + 5)
	public void setOrder(Order order) {
		this.order = order;
	}

}
