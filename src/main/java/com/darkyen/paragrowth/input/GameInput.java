package com.darkyen.paragrowth.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Input processor that offers advanced rebindable keys, system of triggers and saving of the bindings
 */
public final class GameInput implements InputProcessor {

    private final ObjectMap<String, BoundFunction> functions = new ObjectMap<>();
    private boolean somethingChanged = false;

    public GameInput(BoundFunction... functions) {
        final ObjectMap<String, BoundFunction> f = this.functions;
        for (BoundFunction function : functions) {
            if(f.containsKey(function.key))throw new IllegalArgumentException("Duplicate key! "+function.key);
            f.put(function.key, function);
        }
    }

    public GameInput(Iterable<BoundFunction> functions) {
        final ObjectMap<String, BoundFunction> f = this.functions;
        for (BoundFunction function : functions) {
            if(f.containsKey(function.key))throw new IllegalArgumentException("Duplicate key! "+function.key);
            f.put(function.key, function);
        }
    }

    public void setToggle(BoundFunction function, boolean toggle) {
        if (function.toggle == toggle) return;
        function.toggle = toggle;
        somethingChanged = true;
    }

    public void setRepeatTimeout(BoundFunction function, int timeout) {
        if (function.repeatTimeout == timeout) return;
        function.repeatTimeout = timeout;
        somethingChanged = true;
    }

    public void addBinding(BoundFunction function, Binding binding) {
        if (function.realBinding.contains(binding, false)) {
            return;
        }
        function.realBinding.add(binding);
        somethingChanged = true;
    }

    /**
     * Assigns all functions to their preferred bindings. Triggers start working after this.
     */
    public void build() {
        keyBound.clear();
        buttonBound.clear();
        positiveScrollBound = null;
        negativeScrollBound = null;
        for (BoundFunction function : functions.values()) {
            if (function.realBinding.size == 0) {
                function.realBinding.addAll(function.defaultBinding);
            }
            for (Binding binding : function.realBinding) {
                final int value = binding.value;
                switch (binding.type) {
                    case KEYBOARD:
                        if (keyBound.containsKey(value)) {
                            Gdx.app.log("GameInput", "Duplicate binding for key "+value);
                        } else {
                            keyBound.put(value, function);
                        }
                        break;
                    case MOUSE_BUTTON:
                        if (buttonBound.containsKey(value)) {
                            Gdx.app.log("GameInput", "Duplicate binding for button "+value);
                        } else {
                            buttonBound.put(value, function);
                        }
                        break;
                    case MOUSE_WHEEL:
                        if (value > 0) {
                            if (positiveScrollBound == null) {
                                positiveScrollBound = function;
                            } else {
                                Gdx.app.log("GameInput", "Duplicate binding for positive scroll");
                            }
                        } else {
                            if (negativeScrollBound == null) {
                                negativeScrollBound = function;
                            } else {
                                Gdx.app.log("GameInput", "Duplicate binding for negative scroll");
                            }
                        }
                        break;
                }
            }
        }
    }

    //region Input Handling

    private IntMap<BoundFunction> keyBound = new IntMap<>();

    @Override
    public boolean keyDown(int keycode) {
        final BoundFunction function = keyBound.get(keycode);
        return function != null && function.keyPressed();
    }

    @Override
    public boolean keyUp(int keycode) {
        final BoundFunction function = keyBound.get(keycode);
        return function != null && function.keyReleased();
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    private IntMap<BoundFunction> buttonBound = new IntMap<>();

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        final BoundFunction function = buttonBound.get(button);
        return function != null && function.keyPressed();
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        final BoundFunction function = buttonBound.get(button);
        return function != null && function.keyReleased();
    }

    private MouseTrigger mouseTrigger = null;
    private int lastX = Gdx.input.getX(), lastY = Gdx.input.getY();

    public void setMouseTrigger(MouseTrigger mouseTrigger) {
        this.mouseTrigger = mouseTrigger;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return mouseMoved(screenX, screenY, true);
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return mouseMoved(screenX, screenY, false);
    }

    private boolean mouseMoved(int x, int y, boolean drag) {
        boolean result = false;
        if (mouseTrigger != null) {
            result = mouseTrigger.mouseMoved(lastX, lastY, x, y, drag);
        }
        lastX = x;
        lastY = y;
        return result;
    }

    private BoundFunction positiveScrollBound = null;
    private BoundFunction negativeScrollBound = null;

    @Override
    public boolean scrolled(int amount) {
        if (amount == 0) return false;
        BoundFunction function = amount > 0 ? positiveScrollBound : negativeScrollBound;
        //noinspection SimplifiableIfStatement
        if (function == null) return false;
        return function.wheelTurned(Math.abs(amount));
    }
    //endregion

    private enum BindingType {
        /**
         * Value is ID of key
         */
        KEYBOARD {
            @Override
            String toMenuString(int value) {
                String result = Input.Keys.toString(value);
                if (result != null) return result;
                else return "Unrecognized key (" + value + ")";
            }
        },
        /**
         * Value is ID of mouse button
         */
        MOUSE_BUTTON {
            @Override
            String toMenuString(int value) {
                switch (value) {
                    case Input.Buttons.LEFT:
                        return "Left Mouse Button";
                    case Input.Buttons.MIDDLE:
                        return "Middle Mouse Button";
                    case Input.Buttons.RIGHT:
                        return "Right Mouse Button";
                    case Input.Buttons.BACK:
                        return "Back Mouse Button";
                    case Input.Buttons.FORWARD:
                        return "Forward Mouse Button";
                    default:
                        return value + " Mouse Button";
                }
            }
        },
        /**
         * Sign of value is direction of mouse wheel
         */
        MOUSE_WHEEL {
            @Override
            String toMenuString(int value) {
                return value > 0 ? "Scroll Wheel Positive" : "Scroll Wheel Negative";
            }
        };
        //Note: Mouse is not rebindable

        abstract String toMenuString(int value);
    }

    public static final class Binding {
        private final BindingType type;
        private final int value;

        private Binding(BindingType type, int value) {
            this.type = type;
            this.value = value;
        }

        public static Binding bindKeyboard(int key) {
            return new Binding(BindingType.KEYBOARD, key);
        }

        public static Binding bindMouseButton(int button) {
            return new Binding(BindingType.MOUSE_BUTTON, button);
        }

        public static Binding bindScrollWheel(boolean positive) {
            return new Binding(BindingType.MOUSE_WHEEL, positive ? 1 : -1);
        }

        public String toMenuString() {
            return type.toMenuString(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Binding binding = (Binding) o;

            return value == binding.value && type == binding.type;

        }

        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + value;
            return result;
        }
    }

    private static String generateKey(String fromLabel){
        StringBuilder sb = new StringBuilder();
        assert !fromLabel.isEmpty();
        for (int i = 0; i < fromLabel.length(); i++) {
            char c = Character.toLowerCase(fromLabel.charAt(i));
            if(c == ' '){
                sb.append('.');
            }else if((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')){
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static BoundFunction function(String key, String label, Binding...defaultBinding) {
        return new BoundFunction(key, label, defaultBinding);
    }

    public static BoundFunction function(String label, Binding...defaultBinding) {
        return new BoundFunction(generateKey(label), label, defaultBinding);
    }

    public static BoundFunction toggleFunction(String key, String label, Binding...defaultBinding) {
        final BoundFunction boundFunction = new BoundFunction(key, label, defaultBinding);
        boundFunction.toggle = true;
        return boundFunction;
    }

    public static BoundFunction toggleFunction(String label, Binding...defaultBinding) {
        final BoundFunction boundFunction = new BoundFunction(generateKey(label), label, defaultBinding);
        boundFunction.toggle = true;
        return boundFunction;
    }

    public static final class BoundFunction {
        //region Technical
        private final String key;
        private final String humanReadableName;
        private final Binding[] defaultBinding;
        private final Array<Binding> realBinding = new Array<>(true, 4, Binding.class);
        //endregion

        //region Settings
        /**
         * How much time must elapse for "times" to reset
         * In millis.
         * <p>
         * When ZERO (default), too fast triggers can be collapsed into one call with "times" set to whatever times it triggered.
         */
        private int repeatTimeout = DEFAULT_REPEAT_TIMEOUT;
        protected static final int DEFAULT_REPEAT_TIMEOUT = 0;

        /**
         * Whether or not this function works like a toggle
         * When FALSE:
         * trigger(pressed = true) is called when the button is pressed
         * trigger(pressed = false) is called when it is released again
         * When TRUE:
         * trigger(pressed = true) is called when the button is pressed
         * trigger(pressed = false) is called when it is PRESSED again
         * <p>
         * When toggle is true, trigger repeat counting is not supported and always returns 1!
         */
        private boolean toggle = DEFAULT_TOGGLE;
        protected static final boolean DEFAULT_TOGGLE = false;
        //endregion

        //region Internal State
        /**
         * Called when triggered.
         */
        private Trigger trigger;

        private boolean pressed = false;

        private long lastPressed = 0;

        private int times = 1;
        //endregion

        protected BoundFunction(String key, String humanReadableName, Binding[] defaultBinding) {
            this.key = key;
            this.humanReadableName = humanReadableName;
            this.defaultBinding = defaultBinding;
        }

        public BoundFunction listen(Trigger trigger) {
            this.trigger = trigger;
            return this;
        }

        /**
         * Meaningful to call only after build();
         *
         * @return binding to which the function is currently bound to, null if unbound
         */
        public Array<Binding> getBinding() {
            return realBinding;
        }

        /**
         * @return binding to which the function is bound to by default, null if unbound
         */
        public Binding[] getDefaultBinding() {
            return defaultBinding;
        }

        public String getName() {
            return humanReadableName;
        }

        public boolean isPressed() {
            return pressed;
        }

        public int getRepeatTimeout() {
            return repeatTimeout;
        }

        public boolean isToggle() {
            return toggle;
        }

        //region Logic
        private boolean trigger(boolean pressed) {
            this.pressed = pressed;
            return trigger != null && trigger.triggered(times, pressed);
        }

        protected boolean keyPressed() {
            if (!toggle) {
                if (pressed) {
                    //This is weird, ignore
                    return true;
                }

                if (repeatTimeout != 0) {
                    long now = System.currentTimeMillis();
                    if (lastPressed + repeatTimeout > now) {
                        times++;
                    } else {
                        times = 1;
                    }
                    lastPressed = now;
                }

                return trigger(true);
            } else {
                times = 1;
                if (pressed) {
                    return trigger(false);
                } else {
                    return trigger(true);
                }
            }
        }

        protected boolean keyReleased() {
            if (!toggle) {
                //noinspection SimplifiableIfStatement
                if (!pressed) return false;
                return trigger(false);
            } else return false;
        }

        protected boolean wheelTurned(int amount) {
            boolean result = false;
            if (repeatTimeout == 0) {
                //Can collapse the repeats
                if (toggle) {
                    final boolean originalState = pressed;
                    final boolean finalState = ((pressed ? 1 : 0) + amount) % 2 == 1;
                    if (originalState != finalState) {
                        trigger(finalState);
                    }
                    result = true;
                } else {
                    times = amount;
                    result = trigger(true);
                    result = result || trigger(false);
                }
            } else {
                for (int i = 0; i < amount; i++) {
                    result = result || keyPressed();
                    result = result || keyReleased();
                }
            }
            return result;
        }

        //endregion
    }

    @FunctionalInterface
    public interface Trigger {
        /**
         * Called by BoundFunction when something happens.
         *
         * @param times   how many times this was triggered in given window
         * @param pressed true if pressed OR toggle event enabled, false otherwise
         * @return true if the event was accepted, false otherwise. Returning false on PRESS event will cause RELEASE event to not be triggered.
         */
        boolean triggered(int times, boolean pressed);
    }

    @FunctionalInterface
    public interface MouseTrigger {
        /**
         * Called by GameInput
         *
         * @param drag true if any button is pressed during the move
         * @return true if the event was accepted, false otherwise. Has effect when bubbling.
         */
        boolean mouseMoved(int fromX, int fromY, int toX, int toY, boolean drag);
    }

}
