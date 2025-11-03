package jp.gr.java_conf.syntarou.midione.ui.userchoice;

import java.util.ArrayList;

public class BasicUserChoiceElement implements IUserChoiceElement {
    int _label;
    String _label2;
    int _subLabel;
    IShowSubLabel _subLabel2;
    private int _value;
    protected ArrayList<IUserChoiceElement> _children = null;

    public BasicUserChoiceElement(int label, String subLabel, int value) {
        _label = label;
        _subLabel2 = () -> subLabel;
        _value = value;
    }

    public BasicUserChoiceElement(String label, String subLabel, int value) {
        _label2 = label;
        _subLabel2 = () -> subLabel;
        _value = value;
    }

    public BasicUserChoiceElement(int label, IShowSubLabel showSubLabel, int value) {
        _label = label;
        _subLabel2 = showSubLabel;
        _value = value;
    }

    public BasicUserChoiceElement(String label, IShowSubLabel showSubLabel, int value) {
        _label2 = label;
        _subLabel2 = showSubLabel;
        _value = value;
    }

    public BasicUserChoiceElement(int label, int subLabel, int value) {
        _label = label;
        _subLabel = subLabel;
        _value = value;
    }

    public BasicUserChoiceElement(String label, int subLabel, int value) {
        _label2 = label;
        _subLabel = subLabel;
        _value = value;
    }

    public int getValue() {
        return _value;
    }

    public interface IShowSubLabel {
        String show();
    }

    @Override
    public int getNameRes() {
        return _label;
    }

    @Override
    public String getNameText() {
        return _label2;
    }

    @Override
    public int getSubLabel() {
        return _subLabel;
    }

    @Override
    public String getSubLabelText() {
        return _subLabel2 == null ? null : _subLabel2.show();
    }
}
