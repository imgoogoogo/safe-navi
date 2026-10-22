import type { ReactNode, Ref } from 'react';
import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  TextInput,
  View,
  type PressableProps,
  type StyleProp,
  type TextInputInstance,
  type TextInputProps,
  type ViewStyle,
} from 'react-native';
import { router } from 'expo-router';
import { colors, fonts, radius, shadows } from '../theme';
import { BackIcon } from './icons';
import { Text } from './Text';
import type { ReportStatus, ReportType } from '../api/types';
import { REPORT_STATUS_LABEL, REPORT_TYPE_LABEL } from '../api/types';

/** Pressable that dims while pressed. */
export function Touchable({ style, ...rest }: PressableProps & { style?: StyleProp<ViewStyle> }) {
  return <Pressable {...rest} style={({ pressed }) => [style, pressed && { opacity: 0.75 }]} />;
}

// ── Buttons ─────────────────────────────────────────────

type ButtonProps = {
  label: string;
  onPress?: () => void;
  loading?: boolean;
  disabled?: boolean;
  style?: StyleProp<ViewStyle>;
  /** vertical padding; design uses 15–18 depending on screen */
  py?: number;
  fontSize?: number;
};

export function PrimaryButton({ label, onPress, loading, disabled, style, py = 17, fontSize = 17 }: ButtonProps) {
  return (
    <Touchable
      onPress={onPress}
      disabled={disabled || loading}
      style={[styles.primary, { paddingVertical: py }, (disabled || loading) && { opacity: 0.6 }, style]}
    >
      {loading ? (
        <ActivityIndicator color="#FFFFFF" style={{ height: fontSize * 1.45 }} />
      ) : (
        <Text size={fontSize} weight={900} color="#FFFFFF" style={styles.center}>
          {label}
        </Text>
      )}
    </Touchable>
  );
}

export function OutlineButton({
  label,
  onPress,
  style,
  py = 15,
  fontSize = 15,
  tone = 'neutral',
}: ButtonProps & { tone?: 'neutral' | 'muted' | 'danger' }) {
  const toneStyle = {
    neutral: { borderColor: colors.border, color: colors.text2 },
    muted: { borderColor: colors.border, color: colors.text3 },
    danger: { borderColor: colors.dangerBorder, color: colors.danger },
  }[tone];
  return (
    <Touchable onPress={onPress} style={[styles.outline, { paddingVertical: py, borderColor: toneStyle.borderColor }, style]}>
      <Text size={fontSize} weight={700} color={toneStyle.color} style={styles.center}>
        {label}
      </Text>
    </Touchable>
  );
}

export function SoftButton({ label, onPress, danger, style }: { label: string; onPress?: () => void; danger?: boolean; style?: StyleProp<ViewStyle> }) {
  return (
    <Touchable
      onPress={onPress}
      style={[styles.soft, { backgroundColor: danger ? colors.dangerSoft : colors.primarySoft }, style]}
    >
      <Text size={14} weight={700} color={danger ? colors.danger : colors.primary} style={styles.center}>
        {label}
      </Text>
    </Touchable>
  );
}

// ── Badges ──────────────────────────────────────────────

/** Amber report-type badge. `withIcon` = overlay version on photo cards. */
export function TypeBadge({ type, withIcon, style }: { type: ReportType; withIcon?: boolean; style?: StyleProp<ViewStyle> }) {
  return (
    <View style={[styles.badge, withIcon ? styles.badgeIcon : styles.badgePlain, { backgroundColor: colors.amberSoft }, style]}>
      {withIcon && (
        <View style={styles.bang}>
          <Text size={9} weight={700} color="#FFFFFF" style={{ lineHeight: 12 }}>
            !
          </Text>
        </View>
      )}
      <Text size={12} weight={700} color={colors.amberText}>
        {REPORT_TYPE_LABEL[type]}
      </Text>
    </View>
  );
}

const STATUS_TONE: Record<ReportStatus, { fg: string; bg: string }> = {
  RESOLVED: { fg: colors.success, bg: colors.successSoft },
  REVIEWING: { fg: colors.primary, bg: colors.primarySoft },
  RECEIVED: { fg: colors.text3, bg: colors.divider },
};

export function StatusBadge({ status }: { status: ReportStatus }) {
  const t = STATUS_TONE[status];
  return (
    <View style={[styles.badge, styles.badgeStatus, { backgroundColor: t.bg }]}>
      <Text size={12} weight={700} color={t.fg}>
        {REPORT_STATUS_LABEL[status]}
      </Text>
    </View>
  );
}

// ── Layout ──────────────────────────────────────────────

/** Back arrow + 21px title row used on 05 · 07 · 09. */
export function ScreenHeader({ title, right, px = 24 }: { title: string; right?: ReactNode; px?: number }) {
  return (
    <View style={[styles.header, { paddingHorizontal: px }]}>
      <Touchable onPress={() => router.back()} hitSlop={10}>
        <BackIcon />
      </Touchable>
      <Text size={21} weight={900} style={{ flex: 1 }}>
        {title}
      </Text>
      {right}
    </View>
  );
}

export function SectionLabel({ children }: { children: ReactNode }) {
  return (
    <Text size={14} weight={700} color={colors.text2}>
      {children}
    </Text>
  );
}

// ── Inputs ──────────────────────────────────────────────

/** Pill text field used on 06 · 07. */
export function PillInput({ style, ref, ...rest }: TextInputProps & { ref?: Ref<TextInputInstance> }) {
  return (
    <TextInput
      ref={ref}
      placeholderTextColor={colors.placeholder}
      {...rest}
      style={[styles.pillInput, style]}
    />
  );
}

const styles = StyleSheet.create({
  center: { textAlign: 'center' },
  primary: {
    backgroundColor: colors.primary,
    borderRadius: radius.pill,
    boxShadow: shadows.primaryButton,
    alignItems: 'center',
    justifyContent: 'center',
  },
  outline: {
    flex: 1,
    backgroundColor: colors.surface,
    borderWidth: 1.5,
    borderRadius: radius.pill,
  },
  soft: { flex: 1, borderRadius: radius.pill, paddingVertical: 11 },
  badge: { flexDirection: 'row', alignItems: 'center', alignSelf: 'flex-start', borderRadius: radius.pill },
  badgeIcon: { gap: 5, paddingVertical: 4, paddingHorizontal: 11 },
  badgePlain: { paddingVertical: 3, paddingHorizontal: 10 },
  badgeStatus: { paddingVertical: 4, paddingHorizontal: 12 },
  bang: {
    width: 12,
    height: 12,
    borderRadius: 6,
    backgroundColor: colors.amber,
    alignItems: 'center',
    justifyContent: 'center',
  },
  header: { flexDirection: 'row', alignItems: 'center', gap: 16, paddingTop: 20, paddingBottom: 12 },
  pillInput: {
    backgroundColor: colors.surface,
    borderRadius: radius.pill,
    paddingVertical: 16,
    paddingHorizontal: 22,
    fontSize: 14,
    fontFamily: fonts[400],
    color: colors.text,
    boxShadow: shadows.input,
  },
});
