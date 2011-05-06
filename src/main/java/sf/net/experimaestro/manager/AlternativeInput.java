package sf.net.experimaestro.manager;


import sf.net.experimaestro.utils.log.Logger;

public class AlternativeInput extends Input {
	final static Logger LOGGER = Logger.getLogger();
	
	AlternativeType alternativeType;

	public AlternativeInput(QName type, AlternativeType alternativeType) {
		super(type);
		this.alternativeType = alternativeType;
	}

	@Override
	Value newValue() {
		return new AlternativeValue(this, alternativeType);
	}

}
