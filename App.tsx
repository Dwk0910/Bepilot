/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 */

import { Dimensions, Text, ViewStyle } from "react-native";
import { SafeAreaProvider, SafeAreaView } from "react-native-safe-area-context";

function App() {
  return (
      <SafeAreaProvider>
          <SafeAreaView style={styles.view}>
              <Text>Hello, World!</Text>
          </SafeAreaView>
      </SafeAreaProvider>
  );
}

const styles: { view: ViewStyle } = {
    view: {
        flex: 1,
        width: Dimensions.get("window").width,
        alignItems: "center"
    },
};

export default App;
