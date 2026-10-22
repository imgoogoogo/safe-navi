import { Text as RNText, type TextProps, StyleSheet } from 'react-native';
import { colors, fonts, type FontWeight } from '../theme';

type Props = TextProps & {
  size?: number;
  weight?: FontWeight;
  color?: string;
};

/**
 * Noto Sans KR text. Custom fonts in RN need one family per weight,
 * so use `weight` instead of `fontWeight`.
 */
export function Text({ size = 14, weight = 400, color = colors.text, style, ...rest }: Props) {
  return (
    <RNText
      {...rest}
      style={[styles.base, { fontSize: size, fontFamily: fonts[weight], color }, style]}
    />
  );
}

const styles = StyleSheet.create({
  base: { includeFontPadding: false },
});
