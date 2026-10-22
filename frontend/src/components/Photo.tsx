import { useId } from 'react';
import { StyleSheet, View, type StyleProp, type ViewStyle } from 'react-native';
import { Image } from 'expo-image';
import Svg, { Defs, Line, Pattern, Rect } from 'react-native-svg';
import { colors } from '../theme';
import { Text } from './Text';

type Props = {
  uri?: string | null;
  radius?: number;
  placeholder?: string;
  style?: StyleProp<ViewStyle>;
};

/**
 * Report photo. Without a URI it shows the design's 45° stripe placeholder
 * (#E8EDF5 / #D5DEEC, 5px bands).
 */
export function Photo({ uri, radius = 0, placeholder, style }: Props) {
  const id = useId().replace(/:/g, '');
  return (
    <View style={[styles.box, { borderRadius: radius }, style]}>
      {uri ? (
        <Image source={{ uri }} style={StyleSheet.absoluteFill} contentFit="cover" transition={150} />
      ) : (
        <>
          <Svg style={StyleSheet.absoluteFill}>
            <Defs>
              <Pattern id={id} patternUnits="userSpaceOnUse" width={10} height={10} patternTransform="rotate(45)">
                <Rect width={10} height={10} fill="#E8EDF5" />
                <Line x1={7.5} y1={0} x2={7.5} y2={10} stroke="#D5DEEC" strokeWidth={5} />
              </Pattern>
            </Defs>
            <Rect width="100%" height="100%" fill={`url(#${id})`} />
          </Svg>
          {placeholder ? (
            <Text size={11} color="#5A6577" style={styles.label}>
              {placeholder}
            </Text>
          ) : null}
        </>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  box: { overflow: 'hidden', backgroundColor: colors.mapBg, alignItems: 'center', justifyContent: 'center' },
  label: { fontFamily: 'monospace', textAlign: 'center', paddingHorizontal: 8 },
});
