// @ts-nocheck
import { monokai, colors as uiColors } from '@open-hax/uxx/tokens';
function hexToRgbChannels(hex) {
    const normalized = hex.replace('#', '');
    const expanded = normalized.length === 3
        ? normalized.split('').map((char) => char + char).join('')
        : normalized;
    const value = Number.parseInt(expanded, 16);
    const r = (value >> 16) & 255;
    const g = (value >> 8) & 255;
    const b = value & 255;
    return `${r} ${g} ${b}`;
}
function withAlpha(hex) {
    const rgb = hexToRgbChannels(hex);
    return ({ opacityValue }) => opacityValue === undefined ? `rgb(${rgb})` : `rgb(${rgb} / ${opacityValue})`;
}
const config = {
    darkMode: 'class',
    content: ['./index.html', './src/**/*.{ts,tsx}'],
    theme: {
        extend: {
            colors: {
                slate: {
                    50: withAlpha(monokai.fg.bright),
                    100: withAlpha(monokai.fg.default),
                    200: withAlpha(monokai.fg.panel),
                    300: withAlpha(monokai.fg.soft),
                    400: withAlpha(monokai.fg.muted),
                    500: withAlpha(monokai.fg.muted),
                    600: withAlpha(monokai.fg.muted),
                    700: withAlpha(monokai.bg.selection),
                    800: withAlpha(monokai.bg.lighter),
                    900: withAlpha(monokai.bg.default),
                    950: withAlpha(monokai.bg.darker),
                },
                gray: {
                    600: withAlpha(monokai.fg.muted),
                    700: withAlpha(monokai.bg.selection),
                },
                white: withAlpha(monokai.fg.default),
                black: withAlpha(monokai.bg.default),
                blue: {
                    50: withAlpha(monokai.accent.blue),
                    100: withAlpha(monokai.accent.blue),
                    400: withAlpha(monokai.accent.blue),
                    500: withAlpha(monokai.accent.blue),
                    600: withAlpha(monokai.accent.blue),
                    700: withAlpha(monokai.accent.blue),
                },
                cyan: {
                    200: withAlpha(monokai.accent.blue),
                    300: withAlpha(monokai.accent.blue),
                    400: withAlpha(monokai.accent.blue),
                    500: withAlpha(monokai.accent.blue),
                    600: withAlpha(monokai.accent.blue),
                },
                emerald: {
                    200: withAlpha(monokai.accent.green),
                    300: withAlpha(monokai.accent.green),
                    400: withAlpha(monokai.accent.green),
                    500: withAlpha(monokai.accent.green),
                    600: withAlpha(monokai.accent.green),
                },
                green: {
                    600: withAlpha(monokai.accent.green),
                    700: withAlpha(monokai.accent.green),
                },
                amber: {
                    100: withAlpha(monokai.accent.yellow),
                    200: withAlpha(monokai.accent.yellow),
                    300: withAlpha(monokai.accent.orange),
                    400: withAlpha(monokai.accent.orange),
                    500: withAlpha(monokai.accent.orange),
                    600: withAlpha(monokai.accent.orange),
                    700: withAlpha(monokai.accent.orange),
                },
                orange: {
                    500: withAlpha(monokai.accent.orange),
                },
                red: {
                    50: withAlpha(monokai.accent.red),
                    200: withAlpha(monokai.accent.red),
                    300: withAlpha(monokai.accent.red),
                    600: withAlpha(monokai.accent.red),
                    700: withAlpha(monokai.accent.red),
                    800: withAlpha(monokai.accent.red),
                    900: withAlpha(monokai.accent.red),
                },
                rose: {
                    200: withAlpha(monokai.accent.red),
                    300: withAlpha(monokai.accent.red),
                    400: withAlpha(monokai.accent.red),
                    500: withAlpha(monokai.accent.red),
                    600: withAlpha(monokai.accent.red),
                    700: withAlpha(monokai.accent.red),
                },
                violet: {
                    300: withAlpha(monokai.accent.magenta),
                },
                indigo: {
                    300: withAlpha(monokai.accent.magenta),
                    400: withAlpha(monokai.accent.magenta),
                    500: withAlpha(monokai.accent.magenta),
                    600: withAlpha(monokai.accent.magenta),
                },
                surface: withAlpha(monokai.bg.lighter),
                card: withAlpha(monokai.bg.tabInactive),
                accent: withAlpha(monokai.accent.green),
                ink: withAlpha(monokai.fg.default),
            },
            boxShadow: {
                panel: `0 1px 2px ${uiColors.alpha.shadowLight}, 0 10px 30px ${uiColors.alpha.shadow}`,
            },
        },
    },
    plugins: [],
};
export default config;
