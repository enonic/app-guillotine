const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const TerserPlugin = require('terser-webpack-plugin');
const CssMinimizerPlugin = require('css-minimizer-webpack-plugin');
const path = require('path');

const isProd = process.env.NODE_ENV === 'production';

module.exports = {
    context: path.join(__dirname, '/src/main/resources/assets'),
    entry: {
        'js/main': './js/main.tsx',
        'js/query-playground': './js/query-playground.tsx',
        'styles/main': './styles/main.less',
        'styles/query-playground': './styles/query-playground.less',
    },
    output: {
        path: path.join(__dirname, '/build/resources/main/assets'),
        uniqueName: 'app-guillotine',
    },
    resolve: {
        extensions: ['.ts', '.js', '.tsx', '.less', '.css'],
        // graphiql exposes `setup-workers/webpack` only under the `import` condition.
        // ts-loader compiles to CommonJS, so webpack would otherwise resolve with the
        // `require` condition and fail to find it. `'...'` keeps webpack's defaults.
        conditionNames: ['import', '...']
    },
    module: {
        // Monaco/monaco-graphql use many lazy `import()` calls. Without forcing them
        // eager, webpack emits dozens of tiny on-demand chunks (5411.js, 2034.js, ...),
        // each a separate network request. Eager folds them into the parent bundle.
        // Web workers (`new Worker(new URL(...))`) are unaffected and stay separate.
        parser: {
            javascript: {
                dynamicImportMode: 'eager'
            }
        },
        rules: [
            {
                test: /\.tsx?$/,
                use: [{loader: 'ts-loader', options: {configFile: 'tsconfig.json'}}]
            },
            {
                test: /\.css$/,
                use: [
                    {loader: 'style-loader'},
                    {loader: 'css-loader', options: {sourceMap: !isProd}},
                ]
            },
            {
                test: /\.less$/,
                use: [
                    {loader: MiniCssExtractPlugin.loader, options: {publicPath: '../'}},
                    {loader: 'css-loader', options: {sourceMap: !isProd, importLoaders: 1}},
                    {loader: 'postcss-loader', options: {sourceMap: !isProd}},
                    {loader: 'less-loader', options: {sourceMap: !isProd}},
                ]
            },
            {
                test: /\.svg$/,
                type: 'asset/resource',
                generator: {
                    filename: 'img/[base]'
                }
            }
        ]
    },
    optimization: {
        minimizer: [
            new TerserPlugin({
                extractComments: false,
                terserOptions: {
                    compress: {
                        drop_console: false
                    },
                    keep_classnames: true,
                    keep_fnames: true
                }
            }),
            new CssMinimizerPlugin()
        ]
    },
    plugins: [
        new MiniCssExtractPlugin({
            filename: '[name].css',
            chunkFilename: './css/[id].css'
        }),
    ],
    mode: isProd ? 'production' : 'development',
    devtool: isProd ? false : 'source-map',
    performance: {hints: false}
};
